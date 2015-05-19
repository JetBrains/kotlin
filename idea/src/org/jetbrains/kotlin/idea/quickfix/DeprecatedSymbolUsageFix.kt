/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.quickfix

import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiRecursiveElementVisitor
import org.jetbrains.kotlin.analyzer.analyzeInContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.core.refactoring.JetNameSuggester
import org.jetbrains.kotlin.idea.core.refactoring.JetNameValidator
import org.jetbrains.kotlin.idea.imports.canBeReferencedViaImport
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.intentions.setType
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.lexer.JetTokens
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.psiUtil.isAncestor
import org.jetbrains.kotlin.psi.psiUtil.replaced
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.FunctionDescriptorUtil
import org.jetbrains.kotlin.resolve.bindingContextUtil.getDataFlowInfo
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.kotlin.types.JetType
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.ArrayList
import java.util.LinkedHashSet

//TODO: replacement of class usages
//TODO: different replacements for property accessors
//TODO: replace all in project quickfixes on usage and on deprecated annotation
public class DeprecatedSymbolUsageFix(
        element: JetSimpleNameExpression/*TODO?*/,
        val replaceWith: DeprecatedSymbolUsageFix.ReplaceWith
) : JetIntentionAction<JetSimpleNameExpression>(element) {

    //TODO: use ReplaceWith from package kotlin
    private data class ReplaceWith(val expression: String, vararg val imports: String)

    override fun getFamilyName() = "Replace deprecated symbol usage"

    override fun getText() = "Replace with '${replaceWith.expression}'" //TODO: substitute?

    override fun isAvailable(project: Project, editor: Editor?, file: PsiFile): Boolean {
        if (!super.isAvailable(project, editor, file)) return false

        val resolvedCall = element.getResolvedCall(element.analyze()) ?: return false
        if (!resolvedCall.getStatus().isSuccess()) return false
        val descriptor = resolvedCall.getResultingDescriptor()
        if (replaceWithPattern(descriptor) != replaceWith) return false

        try {
            JetPsiFactory(project).createExpression(replaceWith.expression)
            return true
        }
        catch(e: Exception) {
            return false
        }
    }

    override fun invoke(project: Project, editor: Editor?, file: JetFile) {
        val psiFactory = JetPsiFactory(project)
        val bindingContext = element.analyze()
        val resolvedCall = element.getResolvedCall(bindingContext)!!
        val descriptor = resolvedCall.getResultingDescriptor()
        val callExpression = resolvedCall.getCall().getCallElement() as JetExpression
        val qualifiedExpression = callExpression.getParent() as? JetQualifiedExpression
        val expressionToReplace = qualifiedExpression ?: callExpression

        val USER_CODE_KEY = Key<Unit>("USER_CODE")
        val FROM_PARAMETER_KEY = Key<ValueParameterDescriptor>("FROM_PARAMETER")
        val FROM_THIS_KEY = Key<Unit>("FROM_THIS")

        val explicitReceiver = qualifiedExpression?.getReceiverExpression()
        explicitReceiver?.putCopyableUserData(USER_CODE_KEY, Unit)
        explicitReceiver?.putCopyableUserData(FROM_THIS_KEY, Unit)
        //TODO: infix and operator calls

        var (expression, imports, parameterUsages) = replaceWith.toExpression(descriptor.getOriginal(), element.getResolutionFacade(), file, project)

        //TODO: implicit receiver is not always "this"
        //TODO: this@
        for (thisExpression in expression.collectThisExpressions()) {
            if (explicitReceiver != null) {
                thisExpression.replace(explicitReceiver)
            }
            else {
                thisExpression.putCopyableUserData(FROM_THIS_KEY, Unit)
            }
        }

        fun argumentForParameter(parameter: ValueParameterDescriptor): JetExpression? {
            //TODO: optional parameters
            val arguments = resolvedCall.getValueArguments()[parameter] ?: return null //TODO: what if not? vararg?
            return arguments.getArguments().firstOrNull()?.getArgumentExpression() //TODO: what if multiple?
        }

        val introduceValuesForParameters = ArrayList<Pair<ValueParameterDescriptor, JetExpression>>()

        //TODO: check for dropping expressions with potential side effects
        for (parameter in descriptor.getValueParameters()) {
            val argument = argumentForParameter(parameter) ?: continue
            argument.putCopyableUserData(FROM_PARAMETER_KEY, parameter)
            argument.putCopyableUserData(USER_CODE_KEY, Unit)

            val usages = parameterUsages[parameter.getOriginal()]!!
            usages.forEach { it.replace(argument) }

            if (usages.size() > 1 && argument.shouldIntroduceVariableIfUsedTwice()) {
                introduceValuesForParameters.add(parameter to argument)
            }
        }

        if (qualifiedExpression is JetSafeQualifiedExpression) {
            fun processSafeCall() {
                val qualified = expression as? JetQualifiedExpression
                if (qualified != null) {
                    if (qualified.getReceiverExpression().getCopyableUserData(FROM_THIS_KEY) != null) {
                        val selector = qualified.getSelectorExpression()
                        if (selector != null) {
                            expression = psiFactory.createExpressionByPattern("$0?.$1", explicitReceiver!!, selector)
                            return
                        }
                    }
                }

                if (expressionToReplace.isUsedAsExpression(bindingContext)) {
                    val thisReplaced = expression.collectExpressionsWithData(FROM_THIS_KEY, Unit)
                    expression = expression.introduceValue(explicitReceiver!!, expressionToReplace, bindingContext, thisReplaced, safeCall = true)
                }
                else {
                    expression = psiFactory.createExpressionByPattern("if ($0 != null) { $1 }", explicitReceiver!!, expression)
                }
            }
            processSafeCall()
        }

        if (explicitReceiver != null && explicitReceiver.shouldIntroduceVariableIfUsedTwice()) {
            val thisReplaced = expression.collectExpressionsWithData(FROM_THIS_KEY, Unit)
            if (thisReplaced.size() > 1) {
                expression = expression.introduceValue(explicitReceiver, expressionToReplace, bindingContext, thisReplaced)
            }
        }

        for ((parameter, value) in introduceValuesForParameters) {
            val usagesReplaced = expression.collectExpressionsWithData(FROM_PARAMETER_KEY, parameter)
            assert(usagesReplaced.size() > 1)
            expression = expression.introduceValue(value, expressionToReplace, bindingContext, usagesReplaced, nameSuggestion = parameter.getName().asString())
        }

        var result = expressionToReplace.replaced(expression)

        //TODO: drop import of old function (if not needed anymore)?

        for (importFqName in imports) {
            val descriptors = file.getResolutionFacade().resolveImportReference(file, importFqName)
            val descriptorToImport = descriptors.firstOrNull() ?: continue
            ImportInsertHelper.getInstance(project).importDescriptor(file, descriptorToImport)
        }

        val shortenFilter = { element: PsiElement ->
            if (element.getCopyableUserData(USER_CODE_KEY) != null) {
                ShortenReferences.FilterResult.SKIP
            }
            else {
                val thisReceiver = (element as? JetQualifiedExpression)?.getReceiverExpression() as? JetThisExpression
                if (thisReceiver != null && thisReceiver.getCopyableUserData(USER_CODE_KEY) != null) // don't remove explicit 'this' coming from user's code
                    ShortenReferences.FilterResult.GO_INSIDE
                else
                    ShortenReferences.FilterResult.PROCESS
            }
        }
        result = ShortenReferences({ ShortenReferences.Options(removeThis = true) }).process(result, shortenFilter) as JetExpression

        // clean up user data
        result.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                element.putCopyableUserData(USER_CODE_KEY, null)
                element.putCopyableUserData(FROM_PARAMETER_KEY, null)
                element.putCopyableUserData(FROM_THIS_KEY, null)
            }
        })

        val offset = ((result as? JetQualifiedExpression)?.getSelectorExpression() ?: result).getTextOffset()
        editor?.moveCaret(offset)
    }

    private data class ReplacementExpression(
            val expression: JetExpression,
            val imports: Collection<FqName>,
            val parameterUsages: Map<ValueParameterDescriptor, Collection<JetExpression>>
    )

    private fun ReplaceWith.toExpression(
            symbolDescriptor: CallableDescriptor,
            resolutionFacade: ResolutionFacade,
            file: JetFile/*TODO: drop it*/,
            project: Project
    ): ReplacementExpression {
        val psiFactory = JetPsiFactory(project)
        var expression = psiFactory.createExpression(expression)

        val importFqNames = imports
                .filter { FqNameUnsafe.isValid(it) }
                .map { FqNameUnsafe(it) }
                .filter { it.isSafe() }
                .mapTo(LinkedHashSet<FqName>()) { it.toSafe() }

        val symbolScope = getResolutionScope(symbolDescriptor)
        val explicitlyImportedSymbols = importFqNames.flatMap { resolutionFacade.resolveImportReference(file, it) }
        val scope = ChainedScope(symbolDescriptor, "ReplaceWith resolution scope", ExplicitImportsScope(explicitlyImportedSymbols), symbolScope)

        val bindingContext = expression.analyzeInContext(scope)

        val thisType = symbolDescriptor.getExtensionReceiverParameter()?.getType()
                       ?: (symbolDescriptor.getContainingDeclaration() as? ClassifierDescriptor)?.getDefaultType()

        val receiversToAdd = ArrayList<Pair<JetExpression, String>>()

        val parameterUsageKey = Key<ValueParameterDescriptor>("parameterUsageKey")

        expression.accept(object : JetVisitorVoid(){
            override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                val target = bindingContext[BindingContext.REFERENCE_TARGET, expression] ?: return

                if (target.canBeReferencedViaImport()) {
                    if (target.isExtension || expression.getReceiverExpression() == null) {
                        importFqNames.addIfNotNull(target.importableFqName)
                    }
                }

                if (expression.getReceiverExpression() == null) {
                    if (target is ValueParameterDescriptor && target.getContainingDeclaration() == symbolDescriptor) {
                        expression.putCopyableUserData(parameterUsageKey, target)
                    }

                    val resolvedCall = expression.getResolvedCall(bindingContext)
                    if (resolvedCall != null && resolvedCall.getStatus().isSuccess()) {
                        val receiver = if (resolvedCall.getResultingDescriptor().isExtension)
                            resolvedCall.getExtensionReceiver()
                        else
                            resolvedCall.getDispatchReceiver()
                        if (receiver is ThisReceiver) {
                            if (receiver.getType() == thisType) {
                                receiversToAdd.add(expression to "this")
                            }
                            else {
                                val descriptor = receiver.getDeclarationDescriptor()
                                if (descriptor is ClassDescriptor && descriptor.isCompanionObject()) {
                                    receiversToAdd.add(expression to IdeDescriptorRenderers.SOURCE_CODE.renderClassifierName(descriptor))
                                }
                            }
                        }
                    }
                }
            }

            override fun visitJetElement(element: JetElement) {
                element.acceptChildren(this)
            }
        })

        for ((expr, receiverText) in receiversToAdd) {
            val expressionToReplace = expr.getParent() as? JetCallExpression ?: expr
            val newExpr = expressionToReplace.replaced(psiFactory.createExpressionByPattern("$receiverText.$0", expressionToReplace))
            if (expressionToReplace == expression) {
                expression = newExpr
            }
        }

        val parameterUsages = symbolDescriptor.getValueParameters()
                .map { it to expression.collectExpressionsWithData(parameterUsageKey, it) }
                .toMap()

        expression.accept(object : PsiRecursiveElementVisitor() {
            override fun visitElement(element: PsiElement) {
                element.putCopyableUserData(parameterUsageKey, null)
            }
        })

        return ReplacementExpression(expression, importFqNames, parameterUsages)
    }

    private fun getResolutionScope(descriptor: DeclarationDescriptor): JetScope {
        when (descriptor) {
            is PackageFragmentDescriptor -> {
                val moduleDescriptor = descriptor.getContainingDeclaration()
                return getResolutionScope(moduleDescriptor.getPackage(descriptor.fqName)!!)
            }

            is PackageViewDescriptor ->
                return descriptor.getMemberScope()

            is ClassDescriptorWithResolutionScopes ->
                return descriptor.getScopeForMemberDeclarationResolution()

            is FunctionDescriptor ->
                return FunctionDescriptorUtil.getFunctionInnerScope(getResolutionScope(descriptor.getContainingDeclaration()),
                                                                    descriptor, RedeclarationHandler.DO_NOTHING)

            is PropertyDescriptor ->
                return JetScopeUtils.getPropertyDeclarationInnerScope(descriptor,
                                                                      getResolutionScope(descriptor.getContainingDeclaration()!!),
                                                                      RedeclarationHandler.DO_NOTHING)

            else -> throw IllegalArgumentException("Cannot find resolution scope for $descriptor")
        }
    }

    private fun JetExpression.introduceValue(
            value: JetExpression,
            insertDeclarationsBefore: JetExpression,
            bindingContext: BindingContext,
            usages: Collection<JetExpression>,
            nameSuggestion: String? = null,
            safeCall: Boolean = false
    ): JetExpression {
        assert(usages.all { isAncestor(it, strict = true) })

        val psiFactory = JetPsiFactory(this)

        fun nameInCode(name: String) = IdeDescriptorRenderers.SOURCE_CODE.renderName(Name.identifier(name))

        fun replaceUsages(name: String) {
            val nameInCode = psiFactory.createExpression(nameInCode(name))
            for (usage in usages) {
                usage.replace(nameInCode)
            }
        }

        fun suggestName(validator: JetNameValidator): String {
            return if (nameSuggestion != null)
                validator.validateName(nameSuggestion)
            else
                JetNameSuggester.suggestNamesForExpression(value, validator, "t").first()
        }

        // checks that name is used (without receiver) inside this expression but not inside usages that will be replaced
        fun isNameUsed(name: String) = collectNameUsages(this, name).any { nameUsage -> usages.none { it.isAncestor(nameUsage) } }

        if (!safeCall) {
            val block = insertDeclarationsBefore.getParent() as? JetBlockExpression
            if (block != null) {
                val resolutionScope = bindingContext[BindingContext.RESOLUTION_SCOPE, insertDeclarationsBefore]

                val valueType = bindingContext.getType(value)
                var explicitType: JetType? = null
                if (valueType != null && !ErrorUtils.containsErrorType(valueType)) {
                    val valueTypeWithoutExpectedType = value.analyzeInContext(
                            resolutionScope,
                            dataFlowInfo = bindingContext.getDataFlowInfo(insertDeclarationsBefore)
                    ).getType(value)
                    if (valueTypeWithoutExpectedType == null || ErrorUtils.containsErrorType(valueTypeWithoutExpectedType)) {
                        explicitType = valueType
                    }
                }

                val name = suggestName(object : JetNameValidator() {
                    override fun validateInner(name: String): Boolean {
                        return resolutionScope.getLocalVariable(Name.identifier(name)) == null && !isNameUsed(name)
                    }
                })

                var declaration = psiFactory.createDeclaration<JetVariableDeclaration>("val ${nameInCode(name)} = " + value.getText())
                declaration = block.addBefore(declaration, insertDeclarationsBefore) as JetVariableDeclaration
                block.addBefore(psiFactory.createNewLine(), insertDeclarationsBefore)

                if (explicitType != null) {
                    declaration.setType(explicitType)
                }

                replaceUsages(name)
                return this
            }
        }

        val dot = if (safeCall) "?." else "."

        if (!isNameUsed("it")) {
            replaceUsages("it")
            return psiFactory.createExpressionByPattern("$0${dot}let { $1 }", value, this)
        }
        else {
            val name = suggestName(object : JetNameValidator() {
                override fun validateInner(name: String) = !isNameUsed(name)
            })
            replaceUsages(name)
            return psiFactory.createExpressionByPattern("$0${dot}let { ${nameInCode(name)} -> $1 }", value, this)
        }
    }

    private fun <T> JetExpression.collectExpressionsWithData(key: Key<T>, value: T): Collection<JetExpression> {
        val result = ArrayList<JetExpression>()
        this.accept(object : JetVisitorVoid(){
            override fun visitExpression(expression: JetExpression) {
                if (expression.getCopyableUserData(key) == value) {
                    result.add(expression)
                }
                else {
                    super.visitExpression(expression)
                }
            }

            override fun visitJetElement(element: JetElement) {
                element.acceptChildren(this)
            }
        })
        return result
    }

    private fun JetExpression.collectThisExpressions(): Collection<JetThisExpression> {
        val result = ArrayList<JetThisExpression>()
        this.accept(object : JetVisitorVoid(){
            override fun visitThisExpression(expression: JetThisExpression) {
                if (expression.getLabelName() == null) {
                    result.add(expression)
                }
            }

            override fun visitJetElement(element: JetElement) {
                element.acceptChildren(this)
            }
        })
        return result
    }

    private fun collectNameUsages(scope: JetExpression, name: String): ArrayList<JetSimpleNameExpression> {
        val result = ArrayList<JetSimpleNameExpression>()
        scope.accept(object : JetVisitorVoid(){
            override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                if (expression.getReceiverExpression() == null && expression.getReferencedName() == name) {
                    result.add(expression)
                }
            }

            override fun visitJetElement(element: JetElement) {
                element.acceptChildren(this)
            }
        })
        return result
    }

    private fun JetExpression?.shouldIntroduceVariableIfUsedTwice(): Boolean {
        return when (this) {
            is JetSimpleNameExpression -> false
            is JetQualifiedExpression -> getReceiverExpression().shouldIntroduceVariableIfUsedTwice() || getSelectorExpression().shouldIntroduceVariableIfUsedTwice()
            is JetUnaryExpression -> getOperationToken() in setOf(JetTokens.PLUSPLUS, JetTokens.MINUSMINUS) || getBaseExpression().shouldIntroduceVariableIfUsedTwice()
            is JetStringTemplateExpression -> getEntries().any { it is JetStringTemplateEntryWithExpression }
            is JetThisExpression, is JetSuperExpression -> false
            is JetParenthesizedExpression -> getExpression().shouldIntroduceVariableIfUsedTwice()
            is JetBinaryExpression, is JetIfExpression -> true // TODO: discuss it
            else -> true // what else it can be?
        }
    }

    companion object : JetSingleIntentionActionFactory() {
        override fun createAction(diagnostic: Diagnostic): IntentionAction? {
            val nameExpression = diagnostic.getPsiElement() as? JetSimpleNameExpression ?: return null
            val descriptor = Errors.DEPRECATED_SYMBOL_WITH_MESSAGE.cast(diagnostic).getA()
            val replacement = replaceWithPattern(descriptor) ?: return null
            return DeprecatedSymbolUsageFix(nameExpression, replacement)
        }

        private fun replaceWithPattern(descriptor: DeclarationDescriptor): ReplaceWith? {
            val annotationClass = descriptor.builtIns.getDeprecatedAnnotation()
            val annotation = descriptor.getAnnotations().findAnnotation(DescriptorUtils.getFqNameSafe(annotationClass))!!
            val replaceWithValue = annotation.getAllValueArguments().entrySet()
                                           .singleOrNull { it.key.getName().asString() == "replaceWith"/*TODO*/ }
                                           ?.value?.getValue() as? AnnotationDescriptor ?: return null
            val pattern = replaceWithValue.getAllValueArguments().entrySet()
                                  .singleOrNull { it.key.getName().asString() == "expression"/*TODO*/ }
                                  ?.value?.getValue() as? String ?: return null
            if (pattern.isEmpty()) return null
            val argument = replaceWithValue.getAllValueArguments().entrySet().singleOrNull { it.key.getName().asString() == "imports"/*TODO*/ }?.value
            val imports = (argument?.getValue() as? List<CompileTimeConstant<String>>)?.map { it.getValue() } ?: emptyList()
            return ReplaceWith(pattern, *imports.toTypedArray())
        }
    }

    private class ExplicitImportsScope(val descriptors: Collection<DeclarationDescriptor>) : JetScope {
        override fun getClassifier(name: Name) = descriptors.filter { it.getName() == name }.firstIsInstanceOrNull<ClassifierDescriptor>()

        override fun getPackage(name: Name)= descriptors.filter { it.getName() == name }.firstIsInstanceOrNull<PackageViewDescriptor>()

        override fun getProperties(name: Name) = descriptors.filter { it.getName() == name }.filterIsInstance<VariableDescriptor>()

        override fun getLocalVariable(name: Name): VariableDescriptor? = null

        override fun getFunctions(name: Name) = descriptors.filter { it.getName() == name }.filterIsInstance<FunctionDescriptor>()

        override fun getContainingDeclaration(): DeclarationDescriptor {
            throw UnsupportedOperationException()
        }

        override fun getDeclarationsByLabel(labelName: Name): Collection<DeclarationDescriptor> = emptyList()

        override fun getDescriptors(kindFilter: DescriptorKindFilter, nameFilter: (Name) -> Boolean) = descriptors

        override fun getImplicitReceiversHierarchy(): List<ReceiverParameterDescriptor> = emptyList()

        override fun getOwnDeclaredDescriptors(): Collection<DeclarationDescriptor> = emptyList()

        override fun printScopeStructure(p: Printer) {
            p.println(javaClass.getName())
        }
    }
}