package org.jetbrains.jet.plugin.quickfix.createFromUsage.createClass

import org.jetbrains.jet.plugin.quickfix.JetIntentionActionsFactory
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.jet.plugin.quickfix.QuickFixUtil
import java.util.Collections
import org.jetbrains.jet.lang.psi.JetUserType
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor
import org.jetbrains.jet.plugin.caches.resolve.getAnalysisResults
import org.jetbrains.jet.plugin.codeInsight.DescriptorToDeclarationUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiDirectory
import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.lang.psi.JetDelegatorToSuperClass
import org.jetbrains.jet.lang.psi.JetConstructorCalleeExpression
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.plugin.util.ProjectRootsUtil

public object CreateClassFromTypeReferenceActionFactory: JetIntentionActionsFactory() {
    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
        val userType = QuickFixUtil.getParentElementOfType(diagnostic, javaClass<JetUserType>()) ?: return Collections.emptyList()
        val typeArguments = userType.getTypeArgumentsAsTypes()

        val project = userType.getProject()

        val name = userType.getReferencedName() ?: return Collections.emptyList()

        val typeRefParent = userType.getParent()?.getParent()
        if (typeRefParent is JetConstructorCalleeExpression) return Collections.emptyList()

        val traitExpected = typeRefParent is JetDelegatorToSuperClass

        val context = AnalyzerFacadeWithCache.getContextForElement(userType)

        val file = userType.getContainingFile() as? JetFile ?: return Collections.emptyList()

        val isQualifier = (userType.getParent() as? JetUserType)?.let { it.getQualifier() == userType } ?: false
        val qualifier = userType.getQualifier()?.getReferenceExpression()
        val qualifierDescriptor = qualifier?.let { context[BindingContext.REFERENCE_TARGET, it] }

        val targetParent =
                when {
                    qualifier == null -> file

                    qualifierDescriptor is ClassDescriptor -> {
                        DescriptorToDeclarationUtil.getDeclaration(project, qualifierDescriptor)
                    }

                    qualifierDescriptor is PackageViewDescriptor -> {
                        val currentModule = ModuleUtilCore.findModuleForPsiElement(file)
                        val targetFqName = qualifierDescriptor.getFqName()
                        if (targetFqName != file.getPackageFqName()) {
                            JavaPsiFacade.getInstance(project)
                                    .findPackage(targetFqName.asString())
                                    ?.getDirectories()
                                    ?.firstOrNull { ModuleUtilCore.findModuleForPsiElement(it) == currentModule }
                        }
                        else file
                    }

                    else -> null
                } ?: return Collections.emptyList()

        if (!ProjectRootsUtil.isInProjectOrLibSource(targetParent)) return Collections.emptyList()
        if (!(targetParent.isWritable() && (targetParent is PsiDirectory || targetParent is JetElement))) return Collections.emptyList()

        val possibleKinds = when {
            traitExpected -> Collections.singletonList(ClassKind.TRAIT)
            else -> ClassKind.values().filter {
                val noTypeArguments = typeArguments.isEmpty()
                when (it) {
                    ClassKind.OBJECT -> noTypeArguments && isQualifier
                    ClassKind.ANNOTATION_CLASS -> noTypeArguments && !isQualifier
                    ClassKind.ENUM_ENTRY -> false
                    ClassKind.ENUM_CLASS -> noTypeArguments
                    else -> true
                }
            }
        }

        return possibleKinds.map {
            val classInfo = ClassInfo(
                    kind = it,
                    name = name,
                    targetParent = targetParent,
                    expectedTypeInfo = TypeInfo.Empty,
                    typeArguments = typeArguments.map { TypeInfo(it, Variance.INVARIANT) }
            )
            CreateClassFromUsageFix(userType, classInfo)
        }
    }
}