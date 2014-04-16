package org.jetbrains.jet.plugin.completion.smart

import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor
import org.jetbrains.jet.lang.types.JetType
import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import org.jetbrains.jet.lang.descriptors.CallableDescriptor
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns
import org.jetbrains.jet.lang.types.TypeUtils
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.descriptors.ClassKind
import java.util.Collections
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowInfo
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowValue
import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowValueFactory
import java.util.HashMap
import com.google.common.collect.SetMultimap
import org.jetbrains.jet.lang.resolve.calls.autocasts.Nullability
import java.util.HashSet
import org.jetbrains.jet.lang.resolve.BindingContext

class TypesWithAutoCasts(val bindingContext: BindingContext) {
    public fun calculate(expression: JetExpression, receiver: JetExpression?): (DeclarationDescriptor) -> Iterable<JetType> {
        val dataFlowInfo = bindingContext[BindingContext.EXPRESSION_DATA_FLOW_INFO, expression]
        val (variableToTypes: Map<VariableDescriptor, Collection<JetType>>, notNullVariables: Set<VariableDescriptor>)
            = processDataFlowInfo(dataFlowInfo, receiver)

        fun typesOf(descriptor: DeclarationDescriptor): Iterable<JetType> {
            if (descriptor is CallableDescriptor) {
                var returnType = descriptor.getReturnType()
                if (returnType != null && KotlinBuiltIns.getInstance().isNothing(returnType!!)) {
                    //TODO: maybe we should include them on the second press?
                    return listOf()
                }
                if (descriptor is VariableDescriptor) {
                    if (notNullVariables.contains(descriptor) && returnType != null) {
                        returnType = TypeUtils.makeNotNullable(returnType!!)
                    }

                    val autoCastTypes = variableToTypes[descriptor]
                    if (autoCastTypes != null && !autoCastTypes.isEmpty()) {
                        return autoCastTypes + returnType.toList()
                    }
                }
                return returnType.toList()
            }
            else if (descriptor is ClassDescriptor && descriptor.getKind() == ClassKind.ENUM_ENTRY) {
                return listOf(descriptor.getDefaultType())
            }
            else {
                return listOf()
            }
        }

        return ::typesOf
    }

    private data class ProcessDataFlowInfoResult(
            val variableToTypes: Map<VariableDescriptor, Collection<JetType>> = Collections.emptyMap(),
            val notNullVariables: Set<VariableDescriptor> = Collections.emptySet()
    )

    private fun processDataFlowInfo(dataFlowInfo: DataFlowInfo?, receiver: JetExpression?): ProcessDataFlowInfoResult {
        if (dataFlowInfo != null) {
            val dataFlowValueToVariable: (DataFlowValue) -> VariableDescriptor?
            if (receiver != null) {
                val receiverType = bindingContext[BindingContext.EXPRESSION_TYPE, receiver]
                if (receiverType != null) {
                    val receiverId = DataFlowValueFactory.createDataFlowValue(receiver, receiverType, bindingContext).getId()
                    dataFlowValueToVariable = {(value) ->
                        val id = value.getId()
                        if (id is com.intellij.openapi.util.Pair<*, *> && id.first == receiverId) id.second as? VariableDescriptor else null
                    }
                }
                else {
                    return ProcessDataFlowInfoResult()
                }
            }
            else {
                dataFlowValueToVariable = {(value) -> value.getId() as? VariableDescriptor }
            }

            val variableToType = HashMap<VariableDescriptor, Collection<JetType>>()
            val typeInfo: SetMultimap<DataFlowValue, JetType> = dataFlowInfo.getCompleteTypeInfo()
            for ((dataFlowValue, types) in typeInfo.asMap().entrySet()) {
                val variable = dataFlowValueToVariable.invoke(dataFlowValue)
                if (variable != null) {
                    variableToType[variable] = types
                }
            }

            val nullabilityInfo: Map<DataFlowValue, Nullability> = dataFlowInfo.getCompleteNullabilityInfo()
            val notNullVariables = nullabilityInfo
                    .filter { it.getValue() == Nullability.NOT_NULL }
                    .map { dataFlowValueToVariable(it.getKey()) }
                    .filterNotNullTo(HashSet<VariableDescriptor>())

            return ProcessDataFlowInfoResult(variableToType, notNullVariables)
        }

        return ProcessDataFlowInfoResult()
    }
}