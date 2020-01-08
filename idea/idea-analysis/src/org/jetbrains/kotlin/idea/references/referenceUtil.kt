/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.asJava.unwrapped
import org.jetbrains.kotlin.builtins.isExtensionFunctionType
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.caches.resolve.resolveImportReference
import org.jetbrains.kotlin.idea.caches.resolve.resolveToCall
import org.jetbrains.kotlin.idea.imports.canBeReferencedViaImport
import org.jetbrains.kotlin.idea.imports.importableFqName
import org.jetbrains.kotlin.idea.intentions.OperatorToFunctionIntention
import org.jetbrains.kotlin.idea.kdoc.KDocReference
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinFunctionShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinPropertyShortNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTypeAliasShortNameIndex
import org.jetbrains.kotlin.idea.util.CallTypeAndReceiver
import org.jetbrains.kotlin.idea.util.ProjectRootsUtil
import org.jetbrains.kotlin.kdoc.psi.impl.KDocName
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.FunctionImportedFromObject
import org.jetbrains.kotlin.resolve.PropertyImportedFromObject
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VariableAsFunctionResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.isReallySuccess
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedClassDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedPropertyDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedSimpleFunctionDescriptor
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedTypeAliasDescriptor
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions
import org.jetbrains.kotlin.utils.addToStdlib.constant
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstance
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import java.util.*

// Navigation element of the resolved reference
// For property accessor return enclosing property
val PsiReference.unwrappedTargets: Set<PsiElement>
    get() {
        fun PsiElement.adjust(): PsiElement? = when (val target = unwrapped?.originalElement) {
            is KtPropertyAccessor -> target.getNonStrictParentOfType<KtProperty>()
            else -> target
        }

        return when (this) {
            is PsiPolyVariantReference -> multiResolve(false).mapNotNullTo(HashSet<PsiElement>()) { it.element?.adjust() }
            else -> listOfNotNull(resolve()?.adjust()).toSet()
        }
    }

//
fun PsiReference.canBeReferenceTo(candidateTarget: PsiElement): Boolean {
    // optimization
    return element.containingFile == candidateTarget.containingFile
            || ProjectRootsUtil.isInProjectOrLibSource(element, includeScriptsOutsideSourceRoots = true)
}

fun DeclarationDescriptor.findPsiDeclarations(project: Project, resolveScope: GlobalSearchScope): Collection<PsiElement> {
    val fqName = importableFqName ?: return emptyList()

    fun Collection<KtNamedDeclaration>.fqNameFilter() = filter { it.fqName == fqName }
    return when (this) {
        is DeserializedClassDescriptor -> KotlinFullClassNameIndex.getInstance()[fqName.asString(), project, resolveScope]
        is DeserializedTypeAliasDescriptor -> KotlinTypeAliasShortNameIndex.getInstance()[fqName.shortName()
            .asString(), project, resolveScope].fqNameFilter()
        is DeserializedSimpleFunctionDescriptor, is FunctionImportedFromObject -> KotlinFunctionShortNameIndex.getInstance()[fqName.shortName()
            .asString(), project, resolveScope].fqNameFilter()
        is DeserializedPropertyDescriptor, is PropertyImportedFromObject -> KotlinPropertyShortNameIndex.getInstance()[fqName.shortName()
            .asString(), project, resolveScope].fqNameFilter()
        is DeclarationDescriptorWithSource -> listOfNotNull(source.getPsi())
        else -> emptyList()
    }
}

fun PsiReference.matchesTarget(candidateTarget: PsiElement): Boolean {
    if (!canBeReferenceTo(candidateTarget)) return false

    val unwrappedCandidate = candidateTarget.unwrapped?.originalElement ?: return false

    // Optimizations
    when (this) {
        is KtInvokeFunctionReference -> {
            if (candidateTarget !is KtNamedFunction && candidateTarget !is PsiMethod) return false
            if ((candidateTarget as PsiNamedElement).name != OperatorNameConventions.INVOKE.asString()) {
                return false
            }
        }
        is KtDestructuringDeclarationReference -> {
            if (candidateTarget !is KtNamedFunction && candidateTarget !is KtParameter && candidateTarget !is PsiMethod) return false
        }
        is KtSimpleNameReference -> {
            if (unwrappedCandidate is PsiMethod && !canBePsiMethodReference()) return false
        }
    }

    val element = element

    if (candidateTarget is KtImportAlias &&
        (element is KtSimpleNameExpression && element.getReferencedName() == candidateTarget.name ||
                this is KDocReference && this.canonicalText == candidateTarget.name)) {
        val importDirective = candidateTarget.importDirective ?: return false
        val importedFqName = importDirective.importedFqName ?: return false
        val importedDescriptors = importDirective.containingKtFile.resolveImportReference(importedFqName)
        val importableTargets = unwrappedTargets.mapNotNull {
            when {
                it is KtConstructor<*> -> it.containingClassOrObject
                it is PsiMethod && it.isConstructor -> it.containingClass
                else -> it
            }
        }

        val project = element.project
        val resolveScope = element.resolveScope
        return importedDescriptors.any {
            it.findPsiDeclarations(project, resolveScope).any { declaration ->
                declaration in importableTargets
            }
        }
    }

    if (element is KtLabelReferenceExpression) {
        when ((element.parent as? KtContainerNode)?.parent) {
            is KtReturnExpression -> unwrappedTargets.forEach {
                if (it !is KtFunctionLiteral && !(it is KtNamedFunction && it.name.isNullOrEmpty())) return@forEach
                it as KtFunction

                val labeledExpression = it.getLabeledParent(element.getReferencedName())
                if (labeledExpression != null) {
                    if (candidateTarget == labeledExpression) return true else return@forEach
                }
                val calleeReference = it.getCalleeByLambdaArgument()?.mainReference ?: return@forEach
                if (calleeReference.matchesTarget(candidateTarget)) return true
            }
            is KtBreakExpression, is KtContinueExpression -> unwrappedTargets.forEach {
                val labeledExpression = (it as? KtExpression)?.getLabeledParent(element.getReferencedName()) ?: return@forEach
                if (candidateTarget == labeledExpression) return true
            }
        }
    }

    val targets = unwrappedTargets
    val manager = candidateTarget.manager
    if (targets.any { manager.areElementsEquivalent(unwrappedCandidate, it) }) {
        return true
    }

    if (this is KtReference) {
        return targets.any {
            it.isConstructorOf(unwrappedCandidate)
                    || it is KtObjectDeclaration && it.isCompanion() && it.getNonStrictParentOfType<KtClass>() == unwrappedCandidate
        }
    }
    // TODO: Workaround for Kotlin constructor search in Java code. To be removed after refactoring of the search API
    else if (this is PsiJavaCodeReferenceElement && unwrappedCandidate is KtConstructor<*>) {
        var parent = getElement().parent
        if (parent is PsiAnonymousClass) {
            parent = parent.getParent()
        }
        if ((parent as? PsiNewExpression)?.resolveConstructor()?.unwrapped == unwrappedCandidate) return true
    }
    if (this is PsiJavaCodeReferenceElement && candidateTarget is KtObjectDeclaration && unwrappedTargets.size == 1) {
        val referredClass = unwrappedTargets.first()
        if (referredClass is KtClass && candidateTarget in referredClass.companionObjects) {
            if (parent is PsiImportStaticStatement) return true

            return parent.reference?.unwrappedTargets?.any {
                (it is KtProperty || it is KtNamedFunction) && it.parent?.parent == candidateTarget
            } ?: false
        }
    }
    return false
}

fun KtSimpleNameReference.canBePsiMethodReference(): Boolean {
    // NOTE: Accessor references are handled separately, see SyntheticPropertyAccessorReference
    if (element == (element.parent as? KtCallExpression)?.calleeExpression) return true

    val callableReference = element.getParentOfTypeAndBranch<KtCallableReferenceExpression> { callableReference }
    if (callableReference != null) return true

    val binaryOperator = element.getParentOfTypeAndBranch<KtBinaryExpression> { operationReference }
    if (binaryOperator != null) return true

    val unaryOperator = element.getParentOfTypeAndBranch<KtUnaryExpression> { operationReference }
    if (unaryOperator != null) return true

    if (element.getNonStrictParentOfType<KtImportDirective>() != null) return true

    return false
}

private fun PsiElement.isConstructorOf(unwrappedCandidate: PsiElement) =
    when {
        // call to Java constructor
        this is PsiMethod && isConstructor && containingClass == unwrappedCandidate -> true
        // call to Kotlin constructor
        this is KtConstructor<*> && getContainingClassOrObject().isEquivalentTo(unwrappedCandidate) -> true
        else -> false
    }

fun AbstractKtReference<out KtExpression>.renameImplicitConventionalCall(newName: String?): KtExpression {
    if (newName == null) return expression

    val (newExpression, newNameElement) = OperatorToFunctionIntention.convert(expression)
    newNameElement.mainReference.handleElementRename(newName)
    return newExpression
}

val KtSimpleNameExpression.mainReference: KtSimpleNameReference
    get() = references.firstIsInstance()

val KtReferenceExpression.mainReference: KtReference
    get() = if (this is KtSimpleNameExpression) mainReference else references.firstIsInstance<KtReference>()

val KDocName.mainReference: KDocReference
    get() = references.firstIsInstance()

val KtElement.mainReference: KtReference?
    get() = when (this) {
        is KtReferenceExpression -> mainReference
        is KDocName -> mainReference
        else -> references.firstIsInstanceOrNull<KtReference>()
    }

fun KtElement.resolveMainReferenceToDescriptors(): Collection<DeclarationDescriptor> {
    val bindingContext = analyze(BodyResolveMode.PARTIAL)
    return mainReference?.resolveToDescriptors(bindingContext) ?: emptyList()
}

// ----------- Read/write access -----------------------------------------------------------------------------------------------------------------------

enum class ReferenceAccess(val isRead: Boolean, val isWrite: Boolean) {
    READ(true, false), WRITE(false, true), READ_WRITE(true, true)
}

fun KtExpression.readWriteAccess(useResolveForReadWrite: Boolean) = readWriteAccessWithFullExpression(useResolveForReadWrite).first

fun KtExpression.readWriteAccessWithFullExpression(useResolveForReadWrite: Boolean): Pair<ReferenceAccess, KtExpression> {
    var expression = getQualifiedExpressionForSelectorOrThis()
    loop@ while (true) {
        when (val parent = expression.parent) {
            is KtParenthesizedExpression, is KtAnnotatedExpression, is KtLabeledExpression -> expression = parent as KtExpression
            else -> break@loop
        }
    }

    val assignment = expression.getAssignmentByLHS()
    if (assignment != null) {
        when (assignment.operationToken) {
            KtTokens.EQ -> return ReferenceAccess.WRITE to assignment

            else -> {
                if (!useResolveForReadWrite) return ReferenceAccess.READ_WRITE to assignment

                val resolvedCall = assignment.resolveToCall() ?: return ReferenceAccess.READ_WRITE to assignment
                if (!resolvedCall.isReallySuccess()) return ReferenceAccess.READ_WRITE to assignment
                return if (resolvedCall.resultingDescriptor.name in OperatorConventions.ASSIGNMENT_OPERATIONS.values)
                    ReferenceAccess.READ to assignment
                else
                    ReferenceAccess.READ_WRITE to assignment
            }
        }
    }

    val unaryExpression = expression.parent as? KtUnaryExpression
    return if (unaryExpression != null && unaryExpression.operationToken in constant { setOf(KtTokens.PLUSPLUS, KtTokens.MINUSMINUS) })
        ReferenceAccess.READ_WRITE to unaryExpression
    else
        ReferenceAccess.READ to expression
}

fun KtReference.canBeResolvedViaImport(target: DeclarationDescriptor, bindingContext: BindingContext): Boolean {
    if (this is KDocReference) return element.getQualifiedName().size == 1
    return element.canBeResolvedViaImport(target, bindingContext)
}

fun KtElement.canBeResolvedViaImport(target: DeclarationDescriptor, bindingContext: BindingContext): Boolean {
    if (!target.canBeReferencedViaImport()) return false
    if (target.isExtension) return true // assume that any type of reference can use imports when resolved to extension
    if (this !is KtNameReferenceExpression) return false

    val callTypeAndReceiver = CallTypeAndReceiver.detect(this)
    if (callTypeAndReceiver.receiver != null) {
        if (target !is PropertyDescriptor || !target.type.isExtensionFunctionType) return false
        if (callTypeAndReceiver !is CallTypeAndReceiver.DOT && callTypeAndReceiver !is CallTypeAndReceiver.SAFE) return false

        val resolvedCall = bindingContext[BindingContext.CALL, this].getResolvedCall(bindingContext)
                as? VariableAsFunctionResolvedCall ?: return false
        if (resolvedCall.variableCall.explicitReceiverKind.isDispatchReceiver) return false
    }

    if (parent is KtThisExpression || parent is KtSuperExpression) return false // TODO: it's a bad design of PSI tree, we should change it
    return true
}

fun KtFunction.getCalleeByLambdaArgument(): KtSimpleNameExpression? {
    val argument = getParentOfTypeAndBranch<KtValueArgument> { getArgumentExpression() } ?: return null
    val callExpression = when (argument) {
        is KtLambdaArgument -> argument.parent as? KtCallExpression
        else -> (argument.parent as? KtValueArgumentList)?.parent as? KtCallExpression
    } ?: return null
    return callExpression.calleeExpression as? KtSimpleNameExpression
}