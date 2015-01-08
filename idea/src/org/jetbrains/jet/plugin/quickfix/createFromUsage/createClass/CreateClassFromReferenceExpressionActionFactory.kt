package org.jetbrains.jet.plugin.quickfix.createFromUsage.createClass

import org.jetbrains.jet.lang.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.jet.lang.psi.JetTypeReference
import java.util.Collections
import org.jetbrains.jet.plugin.quickfix.JetIntentionActionsFactory
import org.jetbrains.jet.lang.psi.JetClass
import org.jetbrains.jet.lang.psi.JetQualifiedExpression
import org.jetbrains.jet.lang.psi.psiUtil.getAssignmentByLHS
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.psi.JetImportDirective
import org.jetbrains.jet.plugin.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.jet.lang.resolve.calls.callUtil.getCall
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.psi.psiUtil.getQualifiedElementSelector
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.jet.lang.psi.JetReferenceExpression
import java.util.Arrays
import org.jetbrains.jet.lang.psi.JetDotQualifiedExpression
import org.jetbrains.jet.lang.psi.psiUtil.isDotReceiver
import com.intellij.codeInsight.daemon.quickFix.CreateClassOrPackageFix
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor
import org.jetbrains.jet.lang.resolve.name.FqName
import java.util.ArrayList
import org.jetbrains.kotlin.utils.addToStdlib.singletonOrEmptyList

public object CreateClassFromReferenceExpressionActionFactory : JetIntentionActionsFactory() {
    override fun doCreateActions(diagnostic: Diagnostic): List<IntentionAction> {
        val refExpr = diagnostic.getPsiElement() as? JetSimpleNameExpression ?: return Collections.emptyList()
        if (refExpr.getNonStrictParentOfType<JetTypeReference>() != null) return Collections.emptyList()

        val file = refExpr.getContainingFile() as? JetFile ?: return Collections.emptyList()

        val name = refExpr.getReferencedName()

        val (context, moduleDescriptor) = refExpr.analyzeFullyAndGetResult()

        val fullCallExpr = refExpr.getParent()?.let {
            when {
                it is JetCallExpression && it.getCalleeExpression() == refExpr -> return Collections.emptyList()
                it is JetQualifiedExpression && it.getSelectorExpression() == refExpr -> it
                else -> refExpr
            }
        } as? JetExpression ?: return Collections.emptyList()

        val inImport = refExpr.getNonStrictParentOfType<JetImportDirective>() != null
        val qualifierExpected = refExpr.isDotReceiver() || ((refExpr.getParent() as? JetDotQualifiedExpression)?.isDotReceiver() ?: false)

        if (inImport || qualifierExpected) {
            val receiverSelector = (fullCallExpr as? JetQualifiedExpression)?.getReceiverExpression()?.getQualifiedElementSelector() as? JetReferenceExpression
            val qualifierDescriptor = receiverSelector?.let { context[BindingContext.REFERENCE_TARGET, it] }

            val targetParent =
                    getTargetParentByQualifier(refExpr.getContainingJetFile(), receiverSelector != null, qualifierDescriptor)
                    ?: return Collections.emptyList()

            val createPackageAction = refExpr.getCreatePackageFixIfApplicable(targetParent)
            if (createPackageAction != null) return Collections.singletonList(createPackageAction)

            return (if (name.checkClassName()) ClassKind.values() else array())
                    .filter {
                        when (it) {
                            ClassKind.ANNOTATION_CLASS -> inImport
                            ClassKind.ENUM_ENTRY -> inImport && targetParent is JetClass && targetParent.isEnum()
                            else -> true
                        }
                    }
                    .map {
                        val classInfo = ClassInfo(
                                kind = it,
                                name = name,
                                targetParent = targetParent,
                                expectedTypeInfo = TypeInfo.Empty
                        )
                        CreateClassFromUsageFix(refExpr, classInfo)
                    }
        }

        if (fullCallExpr.getAssignmentByLHS() != null) return Collections.emptyList()

        val call = refExpr.getCall(context) ?: return Collections.emptyList()
        val targetParent = getTargetParentByCall(call, file) ?: return Collections.emptyList()
        if (isInnerClassExpected(call)) return Collections.emptyList()

        val (expectedTypeInfo, filter) = fullCallExpr.getInheritableTypeInfo(context, moduleDescriptor, targetParent)

        return Arrays.asList(ClassKind.OBJECT, ClassKind.ENUM_ENTRY)
                .filter {
                    filter(it) && when (it) {
                        ClassKind.OBJECT -> true
                        ClassKind.ENUM_ENTRY -> targetParent is JetClass && targetParent.isEnum()
                        else -> false
                    }
                }
                .map {
                    val classInfo = ClassInfo(
                            kind = it,
                            name = name,
                            targetParent = targetParent,
                            expectedTypeInfo = expectedTypeInfo
                    )
                    CreateClassFromUsageFix(refExpr, classInfo)
                }
    }
}
