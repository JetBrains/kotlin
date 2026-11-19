/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.createDispatchReceiverParameterWithClassParent
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.name.Name

/**
 * Implementation of [AbstractVolatilePropertyAtomicsLowering] for JVM 9+, based on `java.lang.invoke.VarHandle`.
 *
 * Since [java.lang.invoke.VarHandle] methods are signature polymorphic, the calls are generated with exact (unboxed) signatures,
 * which allows the JIT to intrinsify them. All volatile properties are supported, including static fields (properties of objects,
 * companion objects and top-level properties) and `Boolean` properties.
 *
 * The resulting bytecode is equivalent to the following Java code:
 *
 *     class Counter {
 *         private static final /* synthetic */ VarHandle value$VH =
 *             MethodHandles.lookup().unreflectVarHandle(Counter.class.getDeclaredField("value"));
 *         private volatile int value = 0;
 *
 *         public int inc() { return (int) value$VH.getAndAdd(this, 1) + 1; } // invokevirtual VarHandle.getAndAdd(LCounter;I)I
 *     }
 */
internal class VarHandleVolatilePropertyAtomicsLowering(context: JvmBackendContext) : AbstractVolatilePropertyAtomicsLowering(context) {
    override val isApplicable: Boolean
        get() = context.config.target >= JvmTarget.JVM_9

    override fun IrBlockBuilder.getAndSet(access: FieldAccess, newValue: IrExpression): IrExpression =
        varHandleCall(access, "getAndSet", access.erasedType, newValue)

    override fun IrBlockBuilder.compareAndSet(access: FieldAccess, expected: IrExpression, newValue: IrExpression): IrExpression =
        varHandleCall(access, "compareAndSet", irBuiltIns.booleanType, expected, newValue)

    override fun IrBlockBuilder.compareAndExchange(
        access: FieldAccess, expected: () -> IrExpression, newValue: () -> IrExpression,
    ): IrExpression =
        varHandleCall(access, "compareAndExchange", access.erasedType, expected(), newValue())

    override fun IrBlockBuilder.getAndAdd(access: FieldAccess, delta: IrExpression): IrExpression =
        varHandleCall(access, "getAndAdd", access.erasedType, delta)

    /**
     * Generates `VH.<name>(receiver?, args...)` with an exact signature, as javac would do for a signature polymorphic call,
     * see [PolymorphicSignatureLowering].
     */
    private fun IrBuilderWithScope.varHandleCall(
        access: FieldAccess,
        name: String,
        returnType: IrType,
        vararg arguments: IrExpression,
    ): IrExpression {
        val receiver = access.receiver
        val parameterTypes = listOfNotNull(receiver?.let { access.field.parentAsClass.defaultType }) + arguments.map { access.erasedType }
        val method = polymorphicSignatureInstantiation(name, parameterTypes, returnType)
        return irCall(method.symbol, returnType).apply {
            dispatchReceiver = irGetField(null, varHandleFieldFor(access.field))
            var index = 1
            if (receiver != null) this.arguments[index++] = irGet(receiver)
            for (argument in arguments) this.arguments[index++] = argument
        }
    }

    private val polymorphicSignatureInstantiations = mutableMapOf<Triple<String, List<IrType>, IrType>, IrSimpleFunction>()

    private fun polymorphicSignatureInstantiation(name: String, parameterTypes: List<IrType>, returnType: IrType): IrSimpleFunction =
        polymorphicSignatureInstantiations.getOrPut(Triple(name, parameterTypes, returnType)) {
            context.irFactory.buildFun {
                this.name = Name.identifier(name)
                this.returnType = returnType
                origin = JvmLoweredDeclarationOrigin.POLYMORPHIC_SIGNATURE_INSTANTIATION
                isExternal = true
            }.apply {
                parent = symbols.varHandle.owner
                parameters += createDispatchReceiverParameterWithClassParent()
                parameterTypes.forEachIndexed { index, type -> addValueParameter("$$index", type) }
            }
        }

    /** `private static final VarHandle <field>$VH = MethodHandles.lookup().unreflectVarHandle(Host.class.getDeclaredField("<field>"))` */
    private fun varHandleFieldFor(field: IrField): IrField =
        helperFieldFor(field, "\$VH", symbols.varHandle.defaultType, JvmLoweredDeclarationOrigin.VOLATILE_VAR_HANDLE_FIELD) { host ->
            // `lookup()` is caller-sensitive: it's called from `<clinit>` of the host class, so it has private access to the field.
            irCall(symbols.lookupUnreflectVarHandle).apply {
                dispatchReceiver = irCall(symbols.methodHandlesLookup)
                arguments[1] = getDeclaredField(host, field)
            }
        }
}
