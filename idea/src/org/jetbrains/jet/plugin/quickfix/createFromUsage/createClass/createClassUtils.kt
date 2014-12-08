package org.jetbrains.jet.plugin.quickfix.createFromUsage.createClass

import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import com.intellij.psi.PsiElement
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor
import org.jetbrains.jet.plugin.codeInsight.DescriptorToDeclarationUtil
import com.intellij.psi.JavaPsiFacade
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.guessTypes
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.descriptors.ModuleDescriptor
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.types.checker.JetTypeChecker
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.lang.psi.Call
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.jet.lang.resolve.scopes.receivers.Qualifier
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.noSubstitutions
import org.jetbrains.jet.lang.resolve.DescriptorUtils
import org.jetbrains.jet.lang.resolve.DescriptorToSourceUtils
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import com.intellij.codeInsight.daemon.quickFix.CreateClassOrPackageFix
import org.jetbrains.jet.plugin.quickfix.DelegatingIntentionAction
import org.jetbrains.jet.plugin.JetBundle
import com.intellij.psi.PsiPackage
import com.intellij.psi.PsiDirectory
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.plugin.util.ProjectRootsUtil

private fun String.checkClassName(): Boolean = isNotEmpty() && Character.isUpperCase(first())

private fun String.checkPackageName(): Boolean = isNotEmpty() && Character.isLowerCase(first())

private fun getTargetParentByQualifier(
        file: JetFile,
        isQualified: Boolean,
        qualifierDescriptor: DeclarationDescriptor?): PsiElement? {
    val project = file.getProject()
    val targetParent = when {
        !isQualified ->
            file
        qualifierDescriptor is ClassDescriptor ->
            DescriptorToDeclarationUtil.getDeclaration(project, qualifierDescriptor)
        qualifierDescriptor is PackageViewDescriptor ->
            if (qualifierDescriptor.getFqName() != file.getPackageFqName()) {
                JavaPsiFacade.getInstance(project).findPackage(qualifierDescriptor.getFqName().asString())
            }
            else file : PsiElement
        else ->
            null
    } ?: return null
    return if (targetParent.canRefactor()) return targetParent else null
}

private fun getTargetParentByCall(call: Call, file: JetFile): PsiElement? {
    val receiver = call.getExplicitReceiver()
    return when (receiver) {
        ReceiverValue.NO_RECEIVER -> getTargetParentByQualifier(file, false, null)
        is Qualifier -> getTargetParentByQualifier(file, true, receiver.resultingDescriptor)
        else -> getTargetParentByQualifier(file, true, receiver.getType().getConstructor().getDeclarationDescriptor())
    }
}

private fun isInnerClassExpected(call: Call): Boolean {
    val receiver = call.getExplicitReceiver()
    return receiver != ReceiverValue.NO_RECEIVER && receiver !is Qualifier
}

private fun JetExpression.getInheritableTypeInfo(
        context: BindingContext,
        moduleDescriptor: ModuleDescriptor,
        containingDeclaration: PsiElement): Pair<TypeInfo, (ClassKind) -> Boolean> {
    val types = guessTypes(context, moduleDescriptor, false)
    if (types.size != 1) return TypeInfo.Empty to { classKind -> true }

    val type = types.first()
    val descriptor = type.getConstructor().getDeclarationDescriptor()

    val canHaveSubtypes = TypeUtils.canHaveSubtypes(JetTypeChecker.DEFAULT, type)
    val isEnum = DescriptorUtils.isEnumClass(descriptor)

    if (!(canHaveSubtypes || isEnum)
        || descriptor is TypeParameterDescriptor) return TypeInfo.Empty to { classKind -> false }

    return TypeInfo.ByType(type, Variance.OUT_VARIANCE).noSubstitutions() to { classKind ->
        when (classKind) {
            ClassKind.ENUM_ENTRY -> isEnum && containingDeclaration == DescriptorToSourceUtils.descriptorToDeclaration(descriptor)
            else -> canHaveSubtypes
        }
    }
}

private fun PsiElement.canRefactor(): Boolean {
    return when (this) {
        is PsiPackage ->
            getDirectories().any { it.canRefactor() }
        is JetElement, is PsiDirectory ->
            isWritable() && ProjectRootsUtil.isInSource(element = this, includeLibrarySources = false)
        else ->
            false
    }
}

private fun JetSimpleNameExpression.getCreatePackageFixIfApplicable(targetParent: PsiElement): IntentionAction? {
    val name = getReferencedName()
    if (!name.checkPackageName()) return null

    val basePackage: PsiPackage = when (targetParent) {
                                      is JetFile -> JavaPsiFacade.getInstance(targetParent.getProject()).findPackage(targetParent.getPackageFqName().asString())
                                      is PsiPackage -> targetParent : PsiPackage
                                      else -> null
                                  } ?: return null

    val baseName = basePackage.getQualifiedName()
    val fullName = if (baseName.isNotEmpty()) "$baseName.$name" else name

    val javaFix = CreateClassOrPackageFix.createFix(fullName, getResolveScope(), this, basePackage, null, null, null) ?: return null

    return object: DelegatingIntentionAction(javaFix) {
        override fun getFamilyName(): String = JetBundle.message("create.from.usage.family")

        override fun getText(): String = JetBundle.message("create.0.from.usage", "package '${fullName}'")
    }
}