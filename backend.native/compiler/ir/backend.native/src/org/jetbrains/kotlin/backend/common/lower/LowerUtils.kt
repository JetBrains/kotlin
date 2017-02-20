package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BackendContext
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.NonReportingOverrideStrategy
import org.jetbrains.kotlin.resolve.OverridingUtil
import org.jetbrains.kotlin.resolve.descriptorUtil.getSuperClassOrAny
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.utils.Printer

class IrLoweringContext(backendContext: BackendContext) : IrGeneratorContext(backendContext.irBuiltIns)

class DeclarationIrBuilder(backendContext: BackendContext,
                           declarationDescriptor: DeclarationDescriptor,
                           startOffset : Int = UNDEFINED_OFFSET,
                           endOffset : Int = UNDEFINED_OFFSET) :
        IrBuilderWithScope(
                IrLoweringContext(backendContext),
                Scope(declarationDescriptor),
                startOffset,
                endOffset
        )

fun BackendContext.createIrBuilder(declarationDescriptor: DeclarationDescriptor,
                                   startOffset : Int = UNDEFINED_OFFSET,
                                   endOffset : Int = UNDEFINED_OFFSET) =
        DeclarationIrBuilder(this, declarationDescriptor, startOffset, endOffset)


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

open class IrBuildingTransformer(private val context: BackendContext) : IrElementTransformerVoid() {
    private var currentBuilder: IrBuilderWithScope? = null

    protected val builder: IrBuilderWithScope
        get() = currentBuilder!!

    private inline fun <T> withBuilder(declarationDescriptor: DeclarationDescriptor, block: () -> T): T {
        val oldBuilder = currentBuilder
        currentBuilder = context.createIrBuilder(declarationDescriptor)
        return try {
            block()
        } finally {
            currentBuilder = oldBuilder
        }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement {
        withBuilder(declaration.descriptor) {
            return super.visitFunction(declaration)
        }
    }

    override fun visitField(declaration: IrField): IrStatement {
        withBuilder(declaration.descriptor) {
            // Transforms initializer:
            return super.visitField(declaration)
        }
    }

    override fun visitAnonymousInitializer(declaration: IrAnonymousInitializer): IrStatement {
        withBuilder(declaration.descriptor) {
            return super.visitAnonymousInitializer(declaration)
        }
    }
}

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

fun IrConstructor.callsSuper(): Boolean {
    val constructedClass = descriptor.constructedClass
    val superClass = constructedClass.getSuperClassOrAny()
    var callsSuper = false
    var numberOfCalls = 0
    acceptChildrenVoid(object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            element.acceptChildrenVoid(this)
        }

        override fun visitClass(declaration: IrClass) {
            // Skip nested
        }

        override fun visitDelegatingConstructorCall(expression: IrDelegatingConstructorCall) {
            assert(++numberOfCalls == 1, { "More than one delegating constructor call: $descriptor" })
            if (expression.descriptor.constructedClass == superClass)
                callsSuper = true
            else if (expression.descriptor.constructedClass != constructedClass)
                throw AssertionError("Expected either call to another constructor of the class being constructed or" +
                        " call to super class constructor. But was: ${expression.descriptor.constructedClass}")
        }
    })
    assert(numberOfCalls == 1, { "Expected exactly one delegating constructor call but none encountered: $descriptor" })
    return callsSuper
}