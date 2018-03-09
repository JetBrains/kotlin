/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.inspections

import com.intellij.codeInspection.*
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.SmartPsiElementPointer
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.core.KotlinNameSuggester
import org.jetbrains.kotlin.idea.core.ShortenReferences
import org.jetbrains.kotlin.idea.refactoring.getThisLabelName
import org.jetbrains.kotlin.idea.refactoring.rename.KotlinVariableInplaceRenameHandler
import org.jetbrains.kotlin.idea.references.resolveMainReferenceToDescriptors
import org.jetbrains.kotlin.idea.util.getReceiverTargetDescriptor
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.createSmartPointer
import org.jetbrains.kotlin.psi.psiUtil.endOffset
import org.jetbrains.kotlin.psi.psiUtil.getOrCreateParameterList
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContext.FUNCTION
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.scopes.LexicalScope
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.utils.collectDescriptorsFiltered
import org.jetbrains.kotlin.resolve.scopes.utils.findVariable
import org.jetbrains.kotlin.types.KotlinType

private val counterpartNames = mapOf(
    "apply" to "also",
    "run" to "let",
    "also" to "apply",
    "let" to "run"
)

class ScopeFunctionConversionInspection : AbstractKotlinInspection() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean, session: LocalInspectionToolSession): PsiElementVisitor {
        return callExpressionVisitor { expression ->
            val counterpartName = getCounterpart(expression)
            if (counterpartName != null) {
                holder.registerProblem(
                    expression.calleeExpression!!,
                    "Call is replaceable with another scope function",
                    ProblemHighlightType.GENERIC_ERROR_OR_WARNING,
                    if (counterpartName == "also" || counterpartName == "let")
                        ConvertScopeFunctionToParameter(counterpartName)
                    else
                        ConvertScopeFunctionToReceiver(counterpartName)
                )
            }

        }
    }
}

private fun getCounterpart(expression: KtCallExpression): String? {
    val callee = expression.calleeExpression as? KtNameReferenceExpression ?: return null
    val calleeName = callee.getReferencedName()
    val counterpartName = counterpartNames[calleeName]
    val lambdaExpression = expression.lambdaArguments.singleOrNull()?.getLambdaExpression()
    if (counterpartName != null && lambdaExpression != null) {
        if (lambdaExpression.valueParameters.isNotEmpty()) {
            return null
        }
        val bindingContext = callee.analyze(BodyResolveMode.PARTIAL)
        val resolvedCall = callee.getResolvedCall(bindingContext) ?: return null
        if (resolvedCall.resultingDescriptor.fqNameSafe.asString() == "kotlin.$calleeName" &&
            nameResolvesToStdlib(expression, bindingContext, counterpartName)
        ) {
            return counterpartName
        }
    }
    return null
}

private fun nameResolvesToStdlib(expression: KtCallExpression, bindingContext: BindingContext, name: String): Boolean {
    val scope = expression.getResolutionScope(bindingContext) ?: return true
    val descriptors = scope.collectDescriptorsFiltered(nameFilter = { it.asString() == name })
    return descriptors.isNotEmpty() && descriptors.all { it.fqNameSafe.asString() == "kotlin.$name" }
}

class Replacement<T : PsiElement> private constructor(
    private val elementPointer: SmartPsiElementPointer<T>,
    private val replacementFactory: KtPsiFactory.(T) -> PsiElement
) {
    companion object {
        fun <T : PsiElement> create(element: T, replacementFactory: KtPsiFactory.(T) -> PsiElement): Replacement<T> {
            return Replacement(element.createSmartPointer(), replacementFactory)
        }
    }

    fun apply(factory: KtPsiFactory) {
        elementPointer.element?.let {
            it.replace(factory.replacementFactory(it))
        }
    }

    val endOffset
        get() = elementPointer.element!!.endOffset
}

class ReplacementCollection {
    private lateinit var project: Project
    private val replacements = mutableListOf<Replacement<out PsiElement>>()
    var createParameter: KtPsiFactory.() -> PsiElement? = { null }
    var elementToRename: PsiElement? = null

    fun <T : PsiElement> add(element: T, replacementFactory: KtPsiFactory.(T) -> PsiElement) {
        project = element.project
        replacements.add(Replacement.create(element, replacementFactory))
    }

    fun apply() {
        if (replacements.isNotEmpty()) {
            val factory = KtPsiFactory(project)
            elementToRename = factory.createParameter()

            // Calls need to be processed in outside-in order
            replacements.sortBy { it.endOffset }

            for (replacement in replacements) {
                replacement.apply(factory)
            }
        }
    }

    fun isNotEmpty() = replacements.isNotEmpty()
}

abstract class ConvertScopeFunctionFix(private val counterpartName: String) : LocalQuickFix {
    override fun getFamilyName() = "Convert to '$counterpartName'"

    override fun applyFix(project: Project, problemDescriptor: ProblemDescriptor) {
        val callee = problemDescriptor.psiElement as KtNameReferenceExpression
        val callExpression = callee.parent as? KtCallExpression ?: return
        val bindingContext = callExpression.analyze()

        val lambda = callExpression.lambdaArguments.firstOrNull() ?: return
        val functionLiteral = lambda.getLambdaExpression()?.functionLiteral ?: return
        val lambdaDescriptor = bindingContext[FUNCTION, functionLiteral] ?: return

        val replacements = ReplacementCollection()
        analyzeLambda(bindingContext, lambda, lambdaDescriptor, replacements)
        callee.replace(KtPsiFactory(project).createExpression(counterpartName) as KtNameReferenceExpression)
        if (replacements.isNotEmpty()) {
            replacements.apply()
        }
        postprocessLambda(lambda)

        if (replacements.isNotEmpty() && replacements.elementToRename != null && !ApplicationManager.getApplication().isUnitTestMode) {
            replacements.elementToRename!!.startInPlaceRename()
        }
    }

    protected abstract fun postprocessLambda(lambda: KtLambdaArgument)

    protected abstract fun analyzeLambda(
        bindingContext: BindingContext,
        lambda: KtLambdaArgument,
        lambdaDescriptor: SimpleFunctionDescriptor,
        replacements: ReplacementCollection
    )
}

class ConvertScopeFunctionToParameter(counterpartName: String) : ConvertScopeFunctionFix(counterpartName) {
    override fun analyzeLambda(
        bindingContext: BindingContext,
        lambda: KtLambdaArgument,
        lambdaDescriptor: SimpleFunctionDescriptor,
        replacements: ReplacementCollection
    ) {
        val project = lambda.project
        val factory = KtPsiFactory(project)
        val functionLiteral = lambda.getLambdaExpression()?.functionLiteral
        val lambdaExtensionReceiver = lambdaDescriptor.extensionReceiverParameter
        val lambdaDispatchReceiver = lambdaDescriptor.dispatchReceiverParameter

        var parameterName = "it"
        val scopes = mutableSetOf<LexicalScope>()
        if (functionLiteral != null && needUniqueNameForParameter(lambda, scopes)) {
            val parameterType = lambdaExtensionReceiver?.type ?: lambdaDispatchReceiver?.type
            parameterName = findUniqueParameterName(parameterType, scopes)
            replacements.createParameter = {
                val lambdaParameterList = functionLiteral.getOrCreateParameterList()
                val parameterToAdd = createLambdaParameterList(parameterName).parameters.first()
                lambdaParameterList.addParameterBefore(parameterToAdd, lambdaParameterList.parameters.firstOrNull())
            }
        }

        lambda.accept(object : KtTreeVisitorVoid() {
            override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                super.visitSimpleNameExpression(expression)
                val resolvedCall = expression.getResolvedCall(bindingContext) ?: return
                val dispatchReceiverTarget = resolvedCall.dispatchReceiver?.getReceiverTargetDescriptor(bindingContext)
                val extensionReceiverTarget = resolvedCall.extensionReceiver?.getReceiverTargetDescriptor(bindingContext)
                if (dispatchReceiverTarget == lambdaDescriptor || extensionReceiverTarget == lambdaDescriptor) {
                    val parent = expression.parent
                    if (parent is KtCallExpression && expression == parent.calleeExpression) {
                        replacements.add(parent) { element ->
                            factory.createExpressionByPattern("$0.$1", parameterName, element)
                        }
                    } else if (parent is KtQualifiedExpression && parent.receiverExpression is KtThisExpression) {
                        // do nothing
                    } else {
                        val referencedName = expression.getReferencedName()
                        replacements.add(expression) {
                            createExpression("$parameterName.$referencedName")
                        }
                    }
                }
            }

            override fun visitThisExpression(expression: KtThisExpression) {
                val resolvedCall = expression.getResolvedCall(bindingContext) ?: return
                if (resolvedCall.resultingDescriptor == lambdaDispatchReceiver ||
                    resolvedCall.resultingDescriptor == lambdaExtensionReceiver) {
                    replacements.add(expression) { createExpression(parameterName) }
                }
            }
        })
    }

    override fun postprocessLambda(lambda: KtLambdaArgument) {
        ShortenReferences { ShortenReferences.Options(removeThisLabels = true) }.process(lambda) { element ->
            if (element is KtThisExpression && element.getLabelName() != null)
                ShortenReferences.FilterResult.PROCESS
            else
                ShortenReferences.FilterResult.GO_INSIDE
        }
    }

    private fun needUniqueNameForParameter(
        lambdaArgument: KtLambdaArgument,
        scopes: MutableSet<LexicalScope>
    ): Boolean {
        val resolutionScope = lambdaArgument.getResolutionScope()
        scopes.add(resolutionScope)
        var needUniqueName = false
        if (resolutionScope.findVariable(Name.identifier("it"), NoLookupLocation.FROM_IDE) != null) {
            needUniqueName = true
            // Don't return here - we still need to gather the list of nested scopes
        }

        lambdaArgument.accept(object : KtTreeVisitorVoid() {
            override fun visitDeclaration(dcl: KtDeclaration) {
                super.visitDeclaration(dcl)
                checkNeedUniqueName(dcl)
            }

            override fun visitLambdaExpression(lambdaExpression: KtLambdaExpression) {
                super.visitLambdaExpression(lambdaExpression)
                lambdaExpression.bodyExpression?.statements?.firstOrNull()?.let { checkNeedUniqueName(it) }
            }

            private fun checkNeedUniqueName(dcl: KtElement) {
                val nestedResolutionScope = dcl.getResolutionScope()
                scopes.add(nestedResolutionScope)
                if (nestedResolutionScope.findVariable(Name.identifier("it"), NoLookupLocation.FROM_IDE) != null) {
                    needUniqueName = true
                }
            }
        })

        return needUniqueName
    }


    private fun findUniqueParameterName(
        parameterType: KotlinType?,
        resolutionScopes: Collection<LexicalScope>
    ): String {
        fun isNameUnique(parameterName: String): Boolean {
            return resolutionScopes.none { it.findVariable(Name.identifier(parameterName), NoLookupLocation.FROM_IDE) != null }
        }

        return if (parameterType != null)
            KotlinNameSuggester.suggestNamesByType(parameterType, ::isNameUnique).first()
        else {
            KotlinNameSuggester.suggestNameByName("p", ::isNameUnique)
        }
    }
}

class ConvertScopeFunctionToReceiver(counterpartName: String) : ConvertScopeFunctionFix(counterpartName) {
    override fun analyzeLambda(
        bindingContext: BindingContext,
        lambda: KtLambdaArgument,
        lambdaDescriptor: SimpleFunctionDescriptor,
        replacements: ReplacementCollection
    ) {
        lambda.accept(object : KtTreeVisitorVoid() {
            override fun visitSimpleNameExpression(expression: KtSimpleNameExpression) {
                super.visitSimpleNameExpression(expression)
                if (expression.getReferencedName() == "it") {
                    val result = expression.resolveMainReferenceToDescriptors().singleOrNull()
                    if (result is ValueParameterDescriptor && result.containingDeclaration == lambdaDescriptor) {
                        replacements.add(expression) { createThisExpression() }
                    }
                } else {
                    val resolvedCall = expression.getResolvedCall(bindingContext) ?: return
                    val dispatchReceiver = resolvedCall.dispatchReceiver
                    if (dispatchReceiver is ImplicitReceiver) {
                        val parent = expression.parent
                        val thisLabelName = dispatchReceiver.declarationDescriptor.getThisLabelName()
                        if (parent is KtCallExpression && expression == parent.calleeExpression) {
                            replacements.add(parent) { element ->
                                createExpressionByPattern("this@$0.$1", thisLabelName, element)
                            }
                        } else {
                            val referencedName = expression.getReferencedName()
                            replacements.add(expression) {
                                createExpression("this@$thisLabelName.$referencedName")
                            }
                        }
                    }
                }
            }

            override fun visitThisExpression(expression: KtThisExpression) {
                val resolvedCall = expression.getResolvedCall(bindingContext) ?: return
                val qualifierName = resolvedCall.resultingDescriptor.containingDeclaration.name
                replacements.add(expression) { createThisExpression(qualifierName.asString()) }
            }
        })
    }

    override fun postprocessLambda(lambda: KtLambdaArgument) {
        ShortenReferences { ShortenReferences.Options(removeThis = true, removeThisLabels = true) }.process(lambda) { element ->
            if (element is KtThisExpression && element.getLabelName() != null)
                ShortenReferences.FilterResult.PROCESS
            else if (element is KtQualifiedExpression && element.receiverExpression is KtThisExpression)
                ShortenReferences.FilterResult.PROCESS
            else
                ShortenReferences.FilterResult.GO_INSIDE
        }
    }
}

private fun PsiElement.startInPlaceRename() {
    val project = project
    val document = containingFile.viewProvider.document ?: return
    val editor = FileEditorManager.getInstance(project).selectedTextEditor ?: return
    if (editor.document == document) {
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.document)

        editor.caretModel.moveToOffset(startOffset)
        KotlinVariableInplaceRenameHandler().doRename(this, editor, null)
    }
}
