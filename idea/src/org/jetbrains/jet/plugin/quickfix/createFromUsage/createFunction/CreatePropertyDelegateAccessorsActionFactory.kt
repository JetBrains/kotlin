package org.jetbrains.jet.plugin.quickfix.createFromUsage.createFunction

import org.jetbrains.jet.plugin.quickfix.JetSingleIntentionActionFactory
import org.jetbrains.jet.lang.diagnostics.Diagnostic
import com.intellij.codeInsight.intention.IntentionAction
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.TypeInfo
import org.jetbrains.jet.lang.types.Variance
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.ParameterInfo
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.FunctionInfo
import org.jetbrains.jet.lang.psi.JetProperty
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache
import org.jetbrains.jet.lang.resolve.BindingContext
import org.jetbrains.jet.lang.descriptors.PropertyDescriptor
import org.jetbrains.jet.lang.psi.psiUtil.getParentByType
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.plugin.quickfix.createFromUsage.callableBuilder.CallableInfo
import com.intellij.util.SmartList
import org.jetbrains.jet.lang.descriptors.PropertyAccessorDescriptor

object CreatePropertyDelegateAccessorsActionFactory : JetSingleIntentionActionFactory() {
    override fun createAction(diagnostic: Diagnostic): IntentionAction? {
        val expression = diagnostic.getPsiElement() as? JetExpression ?: return null
        val context = AnalyzerFacadeWithCache.getContextForElement(expression)
        [suppress("UNCHECKED_CAST")]

        fun isApplicableForAccessor(accessor: PropertyAccessorDescriptor?): Boolean =
                accessor != null && context[BindingContext.DELEGATED_PROPERTY_RESOLVED_CALL, accessor] == null

        val builtIns = KotlinBuiltIns.getInstance()

        val property = expression.getParentByType(javaClass<JetProperty>()) ?: return null
        val propertyDescriptor = context[BindingContext.DECLARATION_TO_DESCRIPTOR, property] as? PropertyDescriptor
                                 ?: return null

        val propertyReceiver = propertyDescriptor.getExtensionReceiverParameter() ?: propertyDescriptor.getDispatchReceiverParameter()
        val propertyType = propertyDescriptor.getType()

        val accessorReceiverType = TypeInfo(expression, Variance.IN_VARIANCE)
        val thisRefParam = ParameterInfo(TypeInfo(propertyReceiver?.getType() ?: builtIns.getNullableNothingType(), Variance.IN_VARIANCE))
        val metadataParam = ParameterInfo(TypeInfo(builtIns.getPropertyMetadata().getDefaultType(), Variance.IN_VARIANCE))

        val callableInfos = SmartList<CallableInfo>()

        if (isApplicableForAccessor(propertyDescriptor.getGetter())) {
            val getterInfo = FunctionInfo(
                    name = "get",
                    receiverTypeInfo = accessorReceiverType,
                    returnTypeInfo = TypeInfo(propertyType, Variance.OUT_VARIANCE),
                    parameterInfos = listOf(thisRefParam, metadataParam)
            )
            callableInfos.add(getterInfo)
        }

        if (propertyDescriptor.isVar() && isApplicableForAccessor(propertyDescriptor.getSetter())) {
            val newValueParam = ParameterInfo(TypeInfo(propertyType, Variance.IN_VARIANCE))
            val setterInfo = FunctionInfo(
                    name = "set",
                    receiverTypeInfo = accessorReceiverType,
                    returnTypeInfo = TypeInfo(builtIns.getUnitType(), Variance.OUT_VARIANCE),
                    parameterInfos = listOf(thisRefParam, metadataParam, newValueParam)
            )
            callableInfos.add(setterInfo)
        }

        return CreateCallableFromUsageFix(expression, callableInfos)
    }
}