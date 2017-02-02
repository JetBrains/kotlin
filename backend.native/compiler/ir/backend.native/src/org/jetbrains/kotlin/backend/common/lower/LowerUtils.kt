package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.NonReportingOverrideStrategy
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.Printer

class IrLoweringContext(backendContext: BackendContext) : IrGeneratorContext(backendContext.irBuiltIns)

class FunctionIrBuilder(backendContext: BackendContext, functionDescriptor: FunctionDescriptor) :
        IrBuilderWithScope(
                IrLoweringContext(backendContext),
                Scope(functionDescriptor),
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET
        )

fun BackendContext.createFunctionIrBuilder(functionDescriptor: FunctionDescriptor) =
        FunctionIrBuilder(this, functionDescriptor)

fun <T : IrBuilder> T.at(element: IrElement) = this.at(element.startOffset, element.endOffset)

/**
 * Builds [IrBlock] to be used instead of given expression.
 */
inline fun IrGeneratorWithScope.irBlock(expression: IrExpression, origin: IrStatementOrigin? = null,
                                        resultType: KotlinType? = expression.type,
                                        body: IrBlockBuilder.() -> Unit) =
        this.irBlock(expression.startOffset, expression.endOffset, origin, resultType, body)

inline fun IrGeneratorWithScope.irBlockBody(irElement: IrElement, body: IrBlockBodyBuilder.() -> Unit) =
        this.irBlockBody(irElement.startOffset, irElement.endOffset, body)

fun computeOverrides(current: ClassDescriptor, functionsFromCurrent: List<CallableMemberDescriptor>): List<DeclarationDescriptor> {

    val result = mutableListOf<DeclarationDescriptor>()

    val allSuperDescriptors = current.typeConstructor.supertypes
            .flatMap { it.memberScope.getContributedDescriptors() }
            .filterIsInstance<CallableMemberDescriptor>()

    for ((name, group) in allSuperDescriptors.groupBy { it.name }) {
        OverridingUtil.generateOverridesInFunctionGroup(
                name,
                /* membersFromSupertypes = */ group,
                /* membersFromCurrent = */ functionsFromCurrent.filter { it.name == name },
                current,
                object : NonReportingOverrideStrategy() {
                    override fun addFakeOverride(fakeOverride: CallableMemberDescriptor) {
                        result.add(fakeOverride)
                    }

                    override fun conflict(fromSuper: CallableMemberDescriptor, fromCurrent: CallableMemberDescriptor) {
                        error("Conflict in scope of $current: $fromSuper vs $fromCurrent")
                    }
                }
        )
    }

    return result
}

class SimpleMemberScope(val members: List<DeclarationDescriptor>) : MemberScopeImpl() {

    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = TODO()

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> = TODO()

    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> = TODO()

    override fun getContributedDescriptors(kindFilter: DescriptorKindFilter,
                                           nameFilter: (Name) -> Boolean): Collection<DeclarationDescriptor> =
            members.filter { kindFilter.accepts(it) && nameFilter(it.name) }

    override fun printScopeStructure(p: Printer) = TODO("not implemented")

}