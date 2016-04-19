/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.intentions.loopToCallChain

import com.intellij.openapi.util.Key
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.searches.ReferencesSearch
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.diagnostics.Severity
import org.jetbrains.kotlin.idea.analysis.analyzeInContext
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.copied
import org.jetbrains.kotlin.idea.core.replaced
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.intentions.branchedTransformations.isNullExpression
import org.jetbrains.kotlin.idea.references.KtSimpleNameReference
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.references.readWriteAccess
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.nullability
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull

fun generateLambda(inputVariable: KtCallableDeclaration, expression: KtExpression): KtLambdaExpression {
    val psiFactory = KtPsiFactory(expression)

    val lambdaExpression = psiFactory.createExpressionByPattern("{ $0 -> $1 }", inputVariable.nameAsSafeName, expression) as KtLambdaExpression

    val isItUsedInside = expression.anyDescendantOfType<KtNameReferenceExpression> {
        it.getQualifiedExpressionForSelector() == null && it.getReferencedName() == "it"
    }

    if (isItUsedInside) return lambdaExpression

    val resolutionScope = inputVariable.getResolutionScope(inputVariable.analyze(BodyResolveMode.FULL), inputVariable.getResolutionFacade())
    val bindingContext = lambdaExpression.analyzeInContext(resolutionScope, contextExpression = inputVariable)
    val lambdaParam = lambdaExpression.valueParameters.single()
    val lambdaParamDescriptor = bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, lambdaParam]
    val usages = lambdaExpression.collectDescendantsOfType<KtNameReferenceExpression> {
        it.mainReference.resolveToDescriptors(bindingContext).singleOrNull() == lambdaParamDescriptor
    }

    val itExpr = psiFactory.createSimpleName("it")
    for (usage in usages) {
        val replaced = usage.replaced(itExpr)

        // we need to copy user data for checkSmartCastsPreserved() to work
        (usage.node as UserDataHolderBase).copyCopyableDataTo(replaced.node as UserDataHolderBase)
    }

    return psiFactory.createExpressionByPattern("{ $0 }", lambdaExpression.bodyExpression!!) as KtLambdaExpression
}

fun KtExpression?.isTrueConstant()
        = this != null && node?.elementType == KtNodeTypes.BOOLEAN_CONSTANT && text == "true"

fun KtExpression?.isFalseConstant()
        = this != null && node?.elementType == KtNodeTypes.BOOLEAN_CONSTANT && text == "false"

fun KtExpression?.isVariableReference(variable: KtCallableDeclaration): Boolean {
    return this is KtNameReferenceExpression && this.mainReference.isReferenceTo(variable)
}

fun KtExpression?.isSimpleName(name: Name): Boolean {
    return this is KtNameReferenceExpression && this.getQualifiedExpressionForSelector() == null && this.getReferencedNameAsName() == name
}

fun KtCallableDeclaration.hasUsages(inElement: KtElement): Boolean {
    return hasUsages(listOf(inElement))
}

fun KtCallableDeclaration.hasUsages(inElements: Collection<KtElement>): Boolean {
    // TODO: it's a temporary workaround about strange dead-lock when running inspections
    return inElements.any { ReferencesSearch.search(this, LocalSearchScope(it)).any() }
//    return ReferencesSearch.search(this, LocalSearchScope(inElements.toTypedArray())).any()
}

fun KtProperty.hasWriteUsages(): Boolean {
    if (!isVar) return false
    return ReferencesSearch.search(this, useScope).any {
        (it as? KtSimpleNameReference)?.element?.readWriteAccess(useResolveForReadWrite = true)?.isWrite == true
    }
}

fun buildFindOperationGenerator(
        valueIfFound: KtExpression,
        valueIfNotFound: KtExpression,
        inputVariable: KtCallableDeclaration,
        findFirst: Boolean
): ((chainedCallGenerator: ChainedCallGenerator, filter: KtExpression?) -> KtExpression)?  {
    assert(valueIfFound.isPhysical)
    assert(valueIfNotFound.isPhysical)

    fun generateChainedCall(stdlibFunName: String, chainedCallGenerator: ChainedCallGenerator, filter: KtExpression?): KtExpression {
        return if (filter == null) {
            chainedCallGenerator.generate("$stdlibFunName()")
        }
        else {
            val lambda = generateLambda(inputVariable, filter)
            chainedCallGenerator.generate("$stdlibFunName $0:'{}'", lambda)
        }
    }

    val inputVariableCanHoldNull = (inputVariable.resolveToDescriptor() as VariableDescriptor).type.nullability() != TypeNullability.NOT_NULL

    fun ((chainedCallGenerator: ChainedCallGenerator, filter: KtExpression?) -> KtExpression).useElvisOperatorIfNeeded(): ((chainedCallGenerator: ChainedCallGenerator, filter: KtExpression?) -> KtExpression)? {
        if (valueIfNotFound.isNullExpression()) return this

        // we cannot use ?: if found value can be null
        if (inputVariableCanHoldNull) return null

        return { chainedCallGenerator, filter ->
            val generated = this(chainedCallGenerator, filter)
            KtPsiFactory(generated).createExpressionByPattern("$0 ?: $1", generated, valueIfNotFound)
        }
    }

    when {
        valueIfFound.isVariableReference(inputVariable) -> {
            val stdlibFunName = if (findFirst) "firstOrNull" else "lastOrNull"
            val generator = { chainedCallGenerator: ChainedCallGenerator, filter: KtExpression? ->
                generateChainedCall(stdlibFunName, chainedCallGenerator, filter)
            }
            return generator.useElvisOperatorIfNeeded()
        }

        valueIfFound.isTrueConstant() && valueIfNotFound.isFalseConstant() -> {
            return { chainedCallGenerator, filter -> generateChainedCall("any", chainedCallGenerator, filter) }
        }

        valueIfFound.isFalseConstant() && valueIfNotFound.isTrueConstant() -> {
            return { chainedCallGenerator, filter -> generateChainedCall("none", chainedCallGenerator, filter) }
        }

        inputVariable.hasUsages(valueIfFound) -> {
            if (!findFirst) return null // too dangerous because of side effects

            // specially handle the case when the result expression is "<input variable>.<some call>" or "<input variable>?.<some call>"
            val qualifiedExpression = valueIfFound as? KtQualifiedExpression
            if (qualifiedExpression != null) {
                val receiver = qualifiedExpression.receiverExpression
                val selector = qualifiedExpression.selectorExpression
                if (receiver.isVariableReference(inputVariable) && selector != null && !inputVariable.hasUsages(selector)) {
                    return { chainedCallGenerator: ChainedCallGenerator, filter: KtExpression? ->
                        val findFirstCall = generateChainedCall("firstOrNull", chainedCallGenerator, filter)
                        KtPsiFactory(findFirstCall).createExpressionByPattern("$0?.$1", findFirstCall, selector)
                    }.useElvisOperatorIfNeeded()
                }
            }

            // in case of nullable input variable we cannot distinguish by the result of "firstOrNull" whether nothing was found or 'null' was found
            if (inputVariableCanHoldNull) return null

            return { chainedCallGenerator: ChainedCallGenerator, filter: KtExpression? ->
                val findFirstCall = generateChainedCall("firstOrNull", chainedCallGenerator, filter)
                val letBody = generateLambda(inputVariable, valueIfFound)
                KtPsiFactory(findFirstCall).createExpressionByPattern("$0?.let $1:'{}'", findFirstCall, letBody)
            }.useElvisOperatorIfNeeded()
        }

        else -> {
            return { chainedCallGenerator, filter ->
                val chainedCall = generateChainedCall("any", chainedCallGenerator, filter)
                KtPsiFactory(chainedCall).createExpressionByPattern("if ($0) $1 else $2", chainedCall, valueIfFound, valueIfNotFound)
            }
        }
    }
}

fun KtExpressionWithLabel.isBreakOrContinueOfLoop(loop: KtLoopExpression): Boolean {
    val label = getTargetLabel()
    if (label == null) {
        val closestLoop = parents.firstIsInstance<KtLoopExpression>()
        return loop == closestLoop
    }
    else {
        //TODO: does PARTIAL always work here?
        val targetLoop = analyze(BodyResolveMode.PARTIAL)[BindingContext.LABEL_TARGET, label]
        return targetLoop == loop
    }
}

fun KtExpression.previousStatement(): KtExpression? {
    val statement = unwrapIfLabeled()
    if (statement.parent !is KtBlockExpression) return null
    return statement.siblings(forward = false, withItself = false).firstIsInstanceOrNull<KtExpression>()
}

fun KtExpression.nextStatement(): KtExpression? {
    val statement = unwrapIfLabeled()
    if (statement.parent !is KtBlockExpression) return null
    return statement.siblings(forward = true, withItself = false).firstIsInstanceOrNull<KtExpression>()
}

fun KtExpression.unwrapIfLabeled(): KtExpression {
    var statement = this
    while (true) {
        statement = statement.parent as? KtLabeledExpression ?: return statement
    }
}

fun KtLoopExpression.deleteWithLabels() {
    unwrapIfLabeled().delete()
}

fun PsiChildRange.withoutFirstStatement(): PsiChildRange {
    val newFirst = first!!.siblings(forward = true, withItself = false).first { it !is PsiWhiteSpace }
    return PsiChildRange(newFirst, last)
}

fun PsiChildRange.withoutLastStatement(): PsiChildRange {
    val newLast = last!!.siblings(forward = false, withItself = false).first { it !is PsiWhiteSpace }
    return PsiChildRange(first, newLast)
}

data class VariableInitialization(
        val variable: KtProperty,
        val initializationStatement: KtExpression,
        val initializer: KtExpression)

//TODO: we need more correctness checks (if variable is non-local or is local but can be changed by some local functions)
fun KtExpression.detectInitializationBeforeLoop(loop: KtForExpression): VariableInitialization? {
    if (this !is KtNameReferenceExpression) return null
    if (getQualifiedExpressionForSelector() != null) return null
    val variable = this.mainReference.resolve() as? KtProperty ?: return null
    val statementBeforeLoop = loop.previousStatement() //TODO: support initialization not right before the loop

    // do not allow any other usages of this variable inside the loop
    if (ReferencesSearch.search(variable, LocalSearchScope(loop)).count() > 1) return null

    if (statementBeforeLoop == variable) {
        val initializer = variable.initializer ?: return null
        return VariableInitialization(variable, variable, initializer)
    }

    val assignment = statementBeforeLoop?.asAssignment() ?: return null
    if (!assignment.left.isVariableReference(variable)) return null

    val initializer = assignment.right ?: return null
    return VariableInitialization(variable, assignment, initializer)
}

abstract class ReplaceLoopResultTransformation(
        override val loop: KtForExpression,
        final override val inputVariable: KtCallableDeclaration
): ResultTransformation {

    override val commentSavingRange = PsiChildRange.singleElement(loop.unwrapIfLabeled())

    override fun commentRestoringRange(convertLoopResult: KtExpression) = PsiChildRange.singleElement(convertLoopResult)

    override fun convertLoop(resultCallChain: KtExpression): KtExpression {
        return loop.unwrapIfLabeled().replaced(resultCallChain)
    }
}

abstract class AssignToVariableResultTransformation(
        override val loop: KtForExpression,
        final override val inputVariable: KtCallableDeclaration,
        protected val initialization: VariableInitialization
) : ResultTransformation {

    override val commentSavingRange = PsiChildRange(initialization.initializationStatement, loop.unwrapIfLabeled())

    private val commentRestoringRange = commentSavingRange.withoutLastStatement()

    override fun commentRestoringRange(convertLoopResult: KtExpression) = commentRestoringRange

    override fun convertLoop(resultCallChain: KtExpression): KtExpression {
        initialization.initializer.replace(resultCallChain)
        loop.deleteWithLabels()

        if (initialization.variable.isVar && !initialization.variable.hasWriteUsages()) { // change variable to 'val' if possible
            initialization.variable.valOrVarKeyword.replace(KtPsiFactory(initialization.variable).createValKeyword())
        }

        return initialization.initializationStatement
    }
}

class AssignSequenceTransformationResultTransformation(
        private val sequenceTransformation: SequenceTransformation,
        initialization: VariableInitialization
) : AssignToVariableResultTransformation(sequenceTransformation.loop, sequenceTransformation.inputVariable, initialization) {

    override fun generateCode(chainedCallGenerator: ChainedCallGenerator): KtExpression {
        return sequenceTransformation.generateCode(chainedCallGenerator)
    }
}

enum class CollectionKind {
    LIST, SET/*, MAP*/
}

fun KtExpression.isSimpleCollectionInstantiation(): CollectionKind? {
    val callExpression = this as? KtCallExpression ?: return null //TODO: it can be qualified too
    if (callExpression.valueArguments.isNotEmpty()) return null
    val bindingContext = callExpression.analyze(BodyResolveMode.PARTIAL)
    val resolvedCall = callExpression.getResolvedCall(bindingContext) ?: return null
    val constructorDescriptor = resolvedCall.resultingDescriptor as? ConstructorDescriptor ?: return null
    val classDescriptor = constructorDescriptor.containingDeclaration
    val classFqName = classDescriptor.importableFqName?.asString()
    return when (classFqName) {
        "java.util.ArrayList" -> CollectionKind.LIST
        "java.util.HashSet", "java.util.LinkedHashSet" -> CollectionKind.SET
        else -> null
    }
}

fun canChangeLocalVariableType(variable: KtProperty, newTypeText: String, loop: KtForExpression): Boolean {
    val bindingContext = variable.analyze(BodyResolveMode.FULL)

    // analyze the closest block which is not used as expression
    val block = variable.parents
                        .filterIsInstance<KtBlockExpression>()
                        .firstOrNull { bindingContext[BindingContext.USED_AS_EXPRESSION, it] != true }
                ?: return false

    val KEY = Key<Unit>("KEY")
    block.putCopyableUserData(KEY, Unit)
    variable.putCopyableUserData(KEY, Unit)
    loop.putCopyableUserData(KEY, Unit)

    val fileCopy = block.containingFile.copied()
    val blockCopy: KtBlockExpression
    val variableCopy: KtProperty
    val loopCopy: KtForExpression
    try {
        blockCopy = fileCopy.findDescendantOfType<KtBlockExpression> { it.getCopyableUserData(KEY) != null }!!
        variableCopy = blockCopy.findDescendantOfType<KtProperty> { it.getCopyableUserData(KEY) != null }!!
        loopCopy = blockCopy.findDescendantOfType<KtForExpression> { it.getCopyableUserData(KEY) != null }!!
    }
    finally {
        block.putCopyableUserData(KEY, null)
        variable.putCopyableUserData(KEY, null)
        loop.putCopyableUserData(KEY, null)
    }

    variableCopy.typeReference = KtPsiFactory(block).createType(newTypeText)

    val resolutionScope = block.getResolutionScope(bindingContext, block.getResolutionFacade())
    val newBindingContext = blockCopy.analyzeInContext(scope = resolutionScope,
                                                       contextExpression = block,
                                                       dataFlowInfo = bindingContext.getDataFlowInfo(block),
                                                       trace = DelegatingBindingTrace(bindingContext, "Temporary trace"))
    //TODO: what if there were errors before?
    return newBindingContext.diagnostics.none { it.severity == Severity.ERROR && !loopCopy.isAncestor(it.psiElement) }
}
