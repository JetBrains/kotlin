package org.jetbrains.jet.plugin.quickfix.createFromUsage.createClass

import org.jetbrains.jet.lang.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.psi.JetTypeReference
import org.jetbrains.jet.lang.psi.JetCallExpression
import org.jetbrains.jet.lang.psi.JetSimpleNameExpression
import org.jetbrains.jet.lang.psi.JetQualifiedExpression
import org.jetbrains.jet.lang.resolve.calls.callUtil.getCall
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.ParameterInfo
import org.jetbrains.jet.plugin.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.jet.plugin.caches.resolve.analyzeFullyAndGetResult
import org.jetbrains.jet.lang.psi.JetFile
import org.jetbrains.jet.lang.psi.JetAnnotationEntry
import java.util.Collections

public object CreateClassFromConstructorCallActionFactory: JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val diagElement = diagnostic.getPsiElement()
        if (diagElement.getParentByType(javaClass<JetTypeReference>()) != null) return null

        val inAnnotationEntry = diagElement.getParentByType(javaClass<JetAnnotationEntry>()) != null

        val callExpr = diagElement.getParent() as? JetCallExpression ?: return null
        if (callExpr.getCalleeExpression() != diagElement) return null

        val calleeExpr = callExpr.getCalleeExpression() as? JetSimpleNameExpression ?: return null

        val name = calleeExpr.getReferencedName()
        if (!inAnnotationEntry && !name.checkClassName()) return null

        val callParent = callExpr.getParent()
        val fullCallExpr =
                if (callParent is JetQualifiedExpression && callParent.getSelectorExpression() == callExpr) callParent else callExpr

        val file = fullCallExpr.getContainingFile() as? JetFile ?: return null

        val (context, moduleDescriptor) = callExpr.analyzeFullyAndGetResult()

        val call = callExpr.getCall(context) ?: return null
        val targetParent = getTargetParentByCall(call, file) ?: return null
        val inner = isInnerClassExpected(call)

        val valueArguments = callExpr.getValueArguments()
        val defaultParamName = if (inAnnotationEntry && valueArguments.size == 1) "value" else null
        val anyType = KotlinBuiltIns.getInstance().getNullableAnyType()
        val parameterInfos = valueArguments.map {
            ParameterInfo(
                    it.getArgumentExpression()?.let { TypeInfo(it, Variance.IN_VARIANCE) } ?: TypeInfo(anyType, Variance.IN_VARIANCE),
                    it.getArgumentName()?.getReferenceExpression()?.getReferencedName() ?: defaultParamName
            )
        }

        val classKind = if (inAnnotationEntry) ClassKind.ANNOTATION_CLASS else ClassKind.PLAIN_CLASS

        val (expectedTypeInfo, filter) = fullCallExpr.getInheritableTypeInfo(context, moduleDescriptor, targetParent)
        if (!filter(classKind)) return null

        val typeArgumentInfos = if (inAnnotationEntry) {
            Collections.emptyList()
        }
        else {
            callExpr.getTypeArguments().map { TypeInfo(it.getTypeReference(), Variance.INVARIANT) }
        }

        val classInfo = ClassInfo(
                kind = classKind,
                name = name,
                targetParent = targetParent,
                expectedTypeInfo = expectedTypeInfo,
                inner = inner,
                typeArguments = typeArgumentInfos,
                parameterInfos = parameterInfos
        )
        return CreateClassFromUsageFix(callExpr, classInfo)
    }
}