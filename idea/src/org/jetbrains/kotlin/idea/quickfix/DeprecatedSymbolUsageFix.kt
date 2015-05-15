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
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.kotlin.analyzer.analyzeInContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.idea.caches.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.getResolutionFacade
import org.jetbrains.kotlin.idea.imports.canBeReferencedViaImport
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.idea.util.ImportInsertHelper
import org.jetbrains.kotlin.idea.util.ShortenReferences
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.FqNameUnsafe
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getReceiverExpression
import org.jetbrains.kotlin.psi.psiUtil.replaced
import org.jetbrains.kotlin.psi.psiUtil.siblings
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.FunctionDescriptorUtil
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisReceiver
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
        val callElement = resolvedCall.getCall().getCallElement() as JetExpression
        val qualifiedExpression = callElement.getParent() as? JetQualifiedExpression
        val expressionToReplace = qualifiedExpression ?: callElement

        val USER_CODE_KEY = Key<Unit>("USER_CODE")

        val explicitReceiver = qualifiedExpression?.getReceiverExpression()
        explicitReceiver?.putCopyableUserData(USER_CODE_KEY, Unit)
        var thisReplacement = explicitReceiver
        //TODO: infix and operator calls

        var (expression, imports) = replaceWith.toExpression(descriptor.getOriginal(), element.getResolutionFacade(), file, project)

        if (qualifiedExpression is JetSafeQualifiedExpression) {
            fun processSafeCall() {
                val qualified = expression as? JetQualifiedExpression
                if (qualified != null) {
                    val thisReceiver = qualified.getReceiverExpression() as? JetThisExpression
                    if (thisReceiver != null && thisReceiver.getLabelName() == null) {
                        val selector = qualified.getSelectorExpression()
                        if (selector != null) {
                            expression = psiFactory.createExpressionByPattern("this?.$0", selector)
                            return
                        }
                    }
                }

                if (expressionToReplace.isUsedAsExpression(bindingContext)) {
                    expression = psiFactory.createExpressionByPattern("$0?.let { $1 }", explicitReceiver!!, expression)
                    thisReplacement = psiFactory.createExpression("it")
                }
                else {
                    expression = psiFactory.createExpressionByPattern("if ($0 != null) { $1 }", explicitReceiver!!, expression)
                }
            }
            processSafeCall()
        }

        val parametersByName = descriptor.getValueParameters().toMap { it.getName().asString() }
        expression.accept(object : JetVisitorVoid(){
            override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                val qualified = expression.getParent() as? JetDotQualifiedExpression
                if (qualified != null && expression == qualified.getSelectorExpression()) return
                val name = expression.getReferencedName()
                val parameter = parametersByName[name] ?: return //TODO: is this always correct? Lambda inside?
                val arguments = resolvedCall.getValueArguments()[parameter] ?: return //TODO: what if not? vararg?
                val argumentExpression = arguments.getArguments().firstOrNull()?.getArgumentExpression() ?: return //TODO: what if multiple?
                argumentExpression.putCopyableUserData(USER_CODE_KEY, Unit)
                expression.replace(argumentExpression)

                //TODO: check if complex expressions are used twice
                //TODO: check for dropping complex expressions
            }

            override fun visitThisExpression(expression: JetThisExpression) {
                if (expression.getLabelName() != null) return //TODO
                if (thisReplacement != null) {
                    expression.replace(thisReplacement!!)
                }
                //TODO: implicit receiver is not always "this"
            }

            override fun visitJetElement(element: JetElement) {
                // we do not use acceptChildren because it does not work with replacement
                var child: PsiElement? = element.getFirstChild()
                while (child != null) {
                    // whitespace may get invalidated on replace
                    val next = child.siblings(withItself = false).firstOrNull { it !is PsiWhiteSpace }
                    child.accept(this)
                    child = next
                }
            }
        })

        var result = expressionToReplace.replaced(expression)

        //TODO: drop import of old function (if not needed anymore)?

        for (importFqName in imports) {
            val descriptors = file.getResolutionFacade().resolveImportReference(file, importFqName)
            val descriptorToImport = descriptors.firstOrNull() ?: continue
            ImportInsertHelper.getInstance(project).importDescriptor(file, descriptorToImport)
        }

        val shortenFilter = { it: PsiElement ->
            if (it.getCopyableUserData(USER_CODE_KEY) != null) {
                ShortenReferences.FilterResult.SKIP
            }
            else {
                val thisReceiver = (it as? JetQualifiedExpression)?.getReceiverExpression() as? JetThisExpression
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
            }
        })

        val offset = ((result as? JetQualifiedExpression)?.getSelectorExpression() ?: result).getTextOffset()
        editor?.moveCaret(offset)
    }

    private fun ReplaceWith.toExpression(symbolDescriptor: CallableDescriptor, resolutionFacade: ResolutionFacade, file: JetFile/*TODO: drop it*/, project: Project): Pair<JetExpression, Collection<FqName>> {
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

        expression.accept(object : JetVisitorVoid(){
            override fun visitSimpleNameExpression(expression: JetSimpleNameExpression) {
                val target = bindingContext[BindingContext.REFERENCE_TARGET, expression] ?: return

                if (target.canBeReferencedViaImport()) {
                    if (target.isExtension || expression.getReceiverExpression() == null) {
                        importFqNames.addIfNotNull(target.importableFqName)
                    }
                }

                if (expression.getReceiverExpression() == null) {
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

        return expression to importFqNames
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