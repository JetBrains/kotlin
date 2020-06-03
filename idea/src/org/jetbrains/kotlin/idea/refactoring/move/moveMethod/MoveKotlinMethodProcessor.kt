package org.jetbrains.kotlin.idea.refactoring.move.moveMethod

import com.intellij.ide.util.EditorHelper
import com.intellij.openapi.util.Ref
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiReferenceExpression
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.refactoring.BaseRefactoringProcessor
import com.intellij.refactoring.move.MoveMultipleElementsViewDescriptor
import com.intellij.refactoring.util.MoveRenameUsageInfo
import com.intellij.usageView.UsageInfo
import com.intellij.usageView.UsageViewBundle
import com.intellij.usageView.UsageViewDescriptor
import com.intellij.util.IncorrectOperationException
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithVisibility
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.idea.caches.resolve.*
import org.jetbrains.kotlin.idea.core.setVisibility
import org.jetbrains.kotlin.idea.refactoring.move.*
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.KotlinMoveTargetForExistingElement
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.MoveConflictChecker
import org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations.Mover
import org.jetbrains.kotlin.idea.references.mainReference
import org.jetbrains.kotlin.idea.search.projectScope
import org.jetbrains.kotlin.idea.util.getFactoryForImplicitReceiverWithSubtypeOf
import org.jetbrains.kotlin.idea.util.getResolutionScope
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.tail
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.descriptorUtil.isAncestorOf
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitReceiver
import org.jetbrains.kotlin.resolve.scopes.utils.getImplicitReceiversHierarchy
import org.jetbrains.kotlin.util.containingNonLocalDeclaration

class MoveKotlinMethodProcessor(
    private val method: KtNamedFunction,
    private val targetVariable: KtNamedDeclaration,
    private val oldClassParameterNames: Map<KtClass, String>,
    private val openInEditor: Boolean = false
) : BaseRefactoringProcessor(method.project) {
    private val targetClassOrObject: KtClassOrObject = if (targetVariable is KtObjectDeclaration) targetVariable else
        (targetVariable.resolveToDescriptorIfAny() as? CallableDescriptor)?.returnType?.constructor?.declarationDescriptor?.findPsi() as KtClass
    private val factory = KtPsiFactory(myProject)
    private val conflicts = MultiMap<PsiElement, String>()

    override fun createUsageViewDescriptor(usages: Array<out UsageInfo>): UsageViewDescriptor {
        return MoveMultipleElementsViewDescriptor(
            arrayOf(method), (targetClassOrObject.fqName ?: UsageViewBundle.message("default.package.presentable.name")).toString()
        )
    }

    override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean {
        return showConflicts(conflicts, refUsages.get())
    }

    override fun findUsages(): Array<UsageInfo> {
        val changeInfo = ContainerChangeInfo(
            ContainerInfo.Class(method.containingClassOrObject!!.fqName!!),
            ContainerInfo.Class(targetClassOrObject.fqName!!)
        )
        val conflictChecker =
            MoveConflictChecker(myProject, listOf(method), KotlinMoveTargetForExistingElement(targetClassOrObject), method)
        val searchScope = myProject.projectScope()
        val internalUsages = mutableSetOf<UsageInfo>()
        val methodCallUsages = mutableSetOf<UsageInfo>()

        methodCallUsages += ReferencesSearch.search(method, searchScope).mapNotNull { ref ->
            createMoveUsageInfoIfPossible(ref, method, addImportToOriginalFile = true, isInternal = method.isAncestor(ref.element))
        }

        if (targetVariableIsMethodParameter()) {
            internalUsages += ReferencesSearch.search(targetVariable, searchScope).mapNotNull { ref ->
                createMoveUsageInfoIfPossible(ref, targetVariable, addImportToOriginalFile = false, isInternal = true)
            }
        }

        internalUsages += method.getInternalReferencesToUpdateOnPackageNameChange(changeInfo)
        traverseOuterInstanceReferences(method) { internalUsages += it }

        conflictChecker.checkAllConflicts(
            methodCallUsages.filter { it is KotlinMoveUsage && !it.isInternal }.toMutableSet(), internalUsages, conflicts
        )

        if (oldClassParameterNames.size > 1) {
            for (usage in methodCallUsages.filter { it.element is KtNameReferenceExpression || it.element is PsiReferenceExpression }) {
                conflicts.putValue(usage.element, KotlinBundle.message("text.references.to.outer.classes.have.to.be.added.manually"))
            }
        }

        return (internalUsages + methodCallUsages).toTypedArray()
    }

    override fun performRefactoring(usages: Array<out UsageInfo>) {
        val usagesToProcess = mutableListOf<UsageInfo>()

        fun changeMethodSignature() {
            if (targetVariableIsMethodParameter()) {
                val parameterIndex = method.valueParameters.indexOf(targetVariable as KtParameter)
                method.valueParameterList?.removeParameter(parameterIndex)
            }
            for ((ktClass, parameterName) in oldClassParameterNames) {
                method.valueParameterList?.addParameterBefore(
                    factory.createParameter("$parameterName: ${ktClass.nameAsSafeName.identifier}"),
                    method.valueParameters.firstOrNull()
                )
            }
        }

        fun KtNameReferenceExpression.getImplicitReceiver(): KtExpression? {
            val scope = getResolutionScope(this.analyze()) ?: return null
            val descriptor = this.resolveToCall()?.resultingDescriptor ?: return null
            val receiverDescriptor = descriptor.extensionReceiverParameter
                ?: descriptor.dispatchReceiverParameter
                ?: return null
            val expressionFactory = scope.getFactoryForImplicitReceiverWithSubtypeOf(receiverDescriptor.type)
                ?: return null
            val receiverText = if (expressionFactory.isImmediate) "this" else expressionFactory.expressionText
            return factory.createExpression(receiverText)
        }

        fun escalateTargetVariableVisibilityIfNeeded(where: DeclarationDescriptor?) {
            if (where == null || targetVariableIsMethodParameter()) return
            val targetDescriptor = targetVariable.resolveToDescriptorIfAny() as? DeclarationDescriptorWithVisibility
                ?: return
            if (!Visibilities.isVisibleIgnoringReceiver(targetDescriptor, where) && method.manager.isInProject(targetVariable)) {
                targetVariable.setVisibility(KtTokens.PUBLIC_KEYWORD)
            }
        }

        fun correctMethodCall(expression: PsiElement) {
            when (expression) {
                is KtNameReferenceExpression -> {
                    val callExpression = expression.parent as? KtCallExpression ?: return
                    escalateTargetVariableVisibilityIfNeeded(callExpression.containingNonLocalDeclaration()?.resolveToDescriptorIfAny())

                    val oldReceiver = callExpression.getQualifiedExpressionForSelector()?.receiverExpression
                        ?: expression.getImplicitReceiver()
                        ?: return

                    val newReceiver = if (targetVariable is KtObjectDeclaration) {
                        factory.createExpression(targetVariable.nameAsSafeName.identifier)
                    } else if (targetVariableIsMethodParameter()) {
                        val parameterIndex = method.valueParameters.indexOf(targetVariable as KtParameter)
                        if (parameterIndex in callExpression.valueArguments.indices) {
                            val argumentExpression = callExpression.valueArguments[parameterIndex].getArgumentExpression()
                                ?: return
                            callExpression.valueArgumentList?.removeArgument(parameterIndex)
                            argumentExpression
                        } else targetVariable.defaultValue
                    } else {
                        factory.createExpression("${oldReceiver.text}.${targetVariable.nameAsSafeName.identifier}")
                    } ?: return

                    if (method.containingClassOrObject in oldClassParameterNames) {
                        callExpression.valueArgumentList?.addArgumentBefore(
                            factory.createArgument(oldReceiver),
                            callExpression.valueArguments.firstOrNull()
                        )
                    }

                    val resultingExpression = callExpression.getQualifiedExpressionForSelectorOrThis()
                        .replace(factory.createExpressionByPattern("$0.$1", newReceiver.text, callExpression))

                    if (targetVariable is KtObjectDeclaration) {
                        val ref = (resultingExpression as? KtQualifiedExpression)?.receiverExpression?.mainReference ?: return
                        createMoveUsageInfoIfPossible(
                            ref, targetClassOrObject, addImportToOriginalFile = true,
                            isInternal = targetClassOrObject.isAncestor(ref.element)
                        )?.let { usagesToProcess += it }
                    }
                }
                is PsiReferenceExpression -> {
                    val callExpression = expression.parent as? PsiMethodCallExpression ?: return
                    val oldReceiver = callExpression.methodExpression.qualifierExpression ?: return

                    val newReceiver = if (targetVariable is KtObjectDeclaration) {
                        // todo: fix usage of target object (import might be needed)
                        val targetObjectName = targetVariable.fqName?.tail(targetVariable.containingKtFile.packageFqName)?.toString()
                            ?: targetVariable.nameAsSafeName.identifier
                        JavaPsiFacade.getElementFactory(myProject).createExpressionFromText("$targetObjectName.INSTANCE", null)
                    } else if (targetVariableIsMethodParameter()) {
                        val parameterIndex = method.valueParameters.indexOf(targetVariable as KtParameter)
                        val arguments = callExpression.argumentList.expressions
                        if (parameterIndex in arguments.indices) {
                            val argumentExpression = arguments[parameterIndex].copy() ?: return
                            arguments[parameterIndex].delete()
                            argumentExpression
                        } else return
                    } else {
                        val getterName = "get${targetVariable.nameAsSafeName.identifier.capitalize()}"
                        JavaPsiFacade.getElementFactory(myProject).createExpressionFromText("${oldReceiver.text}.$getterName()", null)
                    }

                    if (method.containingClassOrObject in oldClassParameterNames) {
                        callExpression.argumentList.addBefore(oldReceiver, callExpression.argumentList.expressions.firstOrNull())
                    }

                    oldReceiver.replace(newReceiver)
                }
            }
        }

        fun replaceReferenceToTargetWithThis(element: KtExpression) {
            val scope = element.getResolutionScope(element.analyze()) ?: return
            val receivers = scope.getImplicitReceiversHierarchy()
            val receiverText =
                if (receivers.isEmpty() || receivers[0].containingDeclaration == method.containingClassOrObject?.resolveToDescriptorIfAny())
                    "this" else "this@${targetClassOrObject.nameAsSafeName.identifier}"
            element.replace(factory.createExpression(receiverText))
        }

        val (methodCallUsages, internalUsages) = usages.partition { it is MoveRenameUsageInfo && it.referencedElement == method }
        val newInternalUsages = mutableListOf<UsageInfo>()
        val oldInternalUsages = mutableListOf<UsageInfo>()

        try {
            for (usage in methodCallUsages) {
                usage.element?.let { element ->
                    correctMethodCall(element)
                }
            }

            for (usage in internalUsages) {
                val element = usage.element ?: continue

                if (usage is MoveRenameUsageInfo) {
                    if (usage.referencedElement == targetVariable && element is KtNameReferenceExpression) {
                        replaceReferenceToTargetWithThis(element)
                    } else {
                        oldInternalUsages += usage
                    }
                }

                if (usage is SourceInstanceReferenceUsageInfo) {
                    if (usage.member == targetVariable && element is KtNameReferenceExpression) {
                        replaceReferenceToTargetWithThis(
                            (element as? KtThisExpression)?.getQualifiedExpressionForReceiver() ?: element
                        )
                    } else {
                        val receiverText = oldClassParameterNames[usage.sourceOrOuter] ?: continue
                        when (element) {
                            is KtThisExpression -> element.replace(factory.createExpression(receiverText))
                            is KtNameReferenceExpression -> {
                                val elementToReplace = (element.parent as? KtCallExpression) ?: element
                                elementToReplace.replace(factory.createExpressionByPattern("$0.$1", receiverText, elementToReplace))
                            }
                        }
                    }
                }
            }

            changeMethodSignature()
            markInternalUsages(oldInternalUsages)

            val movedMethod = Mover.Default(method, targetClassOrObject)
            val oldToNewMethodMap = mapOf<PsiElement, PsiElement>(method to movedMethod)

            newInternalUsages += restoreInternalUsages(movedMethod, oldToNewMethodMap)
            usagesToProcess += newInternalUsages

            postProcessMoveUsages(usagesToProcess, oldToNewMethodMap)
            if (openInEditor) EditorHelper.openInEditor(movedMethod)
        } catch (e: IncorrectOperationException) {
        } finally {
            cleanUpInternalUsages(oldInternalUsages + newInternalUsages)
        }
    }

    private fun targetVariableIsMethodParameter(): Boolean = targetVariable is KtParameter && !targetVariable.hasValOrVar()

    override fun getCommandName(): String = KotlinBundle.message("text.move.method")
}

internal fun getThisClassesToMembers(method: KtNamedFunction) = traverseOuterInstanceReferences(method)

private fun traverseOuterInstanceReferences(
    method: KtNamedFunction,
    body: (SourceInstanceReferenceUsageInfo) -> Unit = {}
): LinkedHashMap<KtClass, MutableSet<KtNamedDeclaration>> {
    val context = method.analyzeWithContent()
    val containingClassOrObject = method.containingClassOrObject ?: return LinkedHashMap()
    val descriptor = containingClassOrObject.unsafeResolveToDescriptor()

    fun getClassOrObjectAndMemberReferencedBy(reference: KtExpression): Pair<DeclarationDescriptor?, CallableDescriptor?> {
        var classOrObjectDescriptor: DeclarationDescriptor? = null
        var memberDescriptor: CallableDescriptor? = null
        if (reference is KtThisExpression) {
            classOrObjectDescriptor = context[BindingContext.REFERENCE_TARGET, reference.instanceReference]
            if (classOrObjectDescriptor?.isAncestorOf(descriptor, false) == true) {
                memberDescriptor =
                    reference.getQualifiedExpressionForReceiver()?.selectorExpression?.getResolvedCall(context)?.resultingDescriptor
            }
        }
        if (reference is KtNameReferenceExpression) {
            val dispatchReceiver = reference.getResolvedCall(context)?.dispatchReceiver as? ImplicitReceiver
            classOrObjectDescriptor = dispatchReceiver?.declarationDescriptor
            if (classOrObjectDescriptor?.isAncestorOf(descriptor, false) == true) {
                memberDescriptor = reference.getResolvedCall(context)?.resultingDescriptor
            }
        }
        return classOrObjectDescriptor to memberDescriptor
    }

    val thisClassesToMembers = LinkedHashMap<KtClass, MutableSet<KtNamedDeclaration>>()
    method.bodyExpression?.forEachDescendantOfType<KtExpression> { reference ->
        val (classOrObjectDescriptor, memberDescriptor) = getClassOrObjectAndMemberReferencedBy(reference)
        (classOrObjectDescriptor?.findPsi() as? KtClassOrObject)?.let { resolvedClassOrObject ->
            val resolvedMember = memberDescriptor?.findPsi() as? KtNamedDeclaration
            if (resolvedClassOrObject is KtClass) {
                if (resolvedClassOrObject in thisClassesToMembers) thisClassesToMembers[resolvedClassOrObject]?.add(
                    resolvedMember
                        ?: resolvedClassOrObject
                )
                else thisClassesToMembers[resolvedClassOrObject] = mutableSetOf(resolvedMember ?: resolvedClassOrObject)
            }
            body(SourceInstanceReferenceUsageInfo(reference, resolvedClassOrObject, resolvedMember))
        }
    }
    return thisClassesToMembers
}

internal class SourceInstanceReferenceUsageInfo(
    reference: KtExpression, val sourceOrOuter: KtClassOrObject, val member: KtNamedDeclaration?
) : UsageInfo(reference)
