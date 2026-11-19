/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.JvmSymbols
import org.jetbrains.kotlin.backend.jvm.ir.javaClassReference
import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isBoolean
import org.jetbrains.kotlin.ir.types.isInt
import org.jetbrains.kotlin.ir.types.isLong
import org.jetbrains.kotlin.ir.util.defaultType

/**
 * Implementation of [AbstractVolatilePropertyAtomicsLowering] for JVM 1.8, which has no `VarHandle`.
 * It is based on `java.util.concurrent.atomic.Atomic{Integer,Long,Reference}FieldUpdater`.
 *
 * The resulting bytecode is equivalent to the following Java code:
 *
 *     class Counter {
 *         private static final /* synthetic */ AtomicIntegerFieldUpdater value$FU =
 *             AtomicIntegerFieldUpdater.newUpdater(Counter.class, "value");
 *         private volatile int value = 0;
 *
 *         public int inc() { return value$FU.addAndGet(this, 1); }
 *     }
 *
 * Reference fields use `AtomicReferenceFieldUpdater.newUpdater(Host.class, Host.class.getDeclaredField("x").getType(), "x")`:
 * the updater requires the exact (erased) JVM type of the field, which is the simplest to obtain from the field itself.
 *
 * Limitations, compared to [VarHandleVolatilePropertyAtomicsLowering]:
 *  * `compareAndExchange` is not available in field updaters, so it is emulated with a CAS loop
 *    (see [emulatedCompareAndExchange]);
 *  * field updaters only support instance fields; this is fine, since the frontend prohibits atomic operations on properties
 *    stored in static fields (see `FirVolatileIntrinsicApplicabilityChecker`);
 *  * there is no field updater for `boolean` fields, so atomic operations on `Boolean` volatile properties are reported as errors.
 *
 * `load()` and `store()` are plain volatile accesses, and are thus supported for all properties.
 *
 * Field updaters perform access and receiver type checks on each call; HotSpot usually eliminates them after inlining,
 * but the code is expected to be somewhat slower than with `VarHandle`.
 */
internal class FieldUpdaterVolatilePropertyAtomicsLowering(context: JvmBackendContext) : AbstractVolatilePropertyAtomicsLowering(context) {
    override val isApplicable: Boolean
        get() = context.config.target < JvmTarget.JVM_9

    override fun unsupportedPropertyKind(field: IrField): String? =
        if (field.type.isBoolean()) "'Boolean' volatile properties" else null

    private val FieldAccess.updater: JvmSymbols.FieldUpdaterSymbols
        get() = when {
            field.type.isInt() -> symbols.atomicIntegerFieldUpdater
            field.type.isLong() -> symbols.atomicLongFieldUpdater
            else -> symbols.atomicReferenceFieldUpdater
        }

    override fun IrBlockBuilder.getAndSet(access: FieldAccess, newValue: IrExpression): IrExpression =
        updaterCall(access, access.updater.getAndSet, newValue)

    override fun IrBlockBuilder.compareAndSet(access: FieldAccess, expected: IrExpression, newValue: IrExpression): IrExpression =
        updaterCall(access, access.updater.compareAndSet, expected, newValue)

    override fun IrBlockBuilder.compareAndExchange(
        access: FieldAccess, expected: () -> IrExpression, newValue: () -> IrExpression,
    ): IrExpression =
        emulatedCompareAndExchange(access, expected, newValue)

    override fun IrBlockBuilder.getAndAdd(access: FieldAccess, delta: IrExpression): IrExpression =
        updaterCall(access, access.updater.getAndAdd, delta)

    override fun IrBlockBuilder.addAndGet(access: FieldAccess, delta: () -> IrExpression): IrExpression =
        updaterCall(access, access.updater.addAndGet, delta())

    /** `<field>$FU.<method>(receiver, args...)` */
    private fun IrBuilderWithScope.updaterCall(
        access: FieldAccess,
        method: IrSimpleFunctionSymbol,
        vararg arguments: IrExpression,
    ): IrExpression {
        return irCall(method).apply {
            dispatchReceiver = irGetField(null, updaterFieldFor(access))
            this.arguments[1] = irGet(access.receiver)
            arguments.forEachIndexed { index, argument -> this.arguments[index + 2] = argument }
        }
    }

    /**
     * `private static final Atomic*FieldUpdater <field>$FU = Atomic*FieldUpdater.newUpdater(Host.class, [type,] "<field>")`
     */
    private fun updaterFieldFor(access: FieldAccess): IrField {
        val field = access.field
        val updater = access.updater
        return helperFieldFor(field, "\$FU", updater.klass.defaultType, JvmLoweredDeclarationOrigin.VOLATILE_FIELD_UPDATER_FIELD) { host ->
            // `newUpdater` is caller-sensitive: it's called from `<clinit>` of the host class, so it has private access to the field.
            irCall(updater.newUpdater).apply {
                arguments[0] = javaClassReference(host.defaultType)
                if (updater === symbols.atomicReferenceFieldUpdater) {
                    arguments[1] = irCall(symbols.javaLangReflectSymbols.javaLangReflectFieldGetType).apply {
                        dispatchReceiver = getDeclaredField(host, field)
                    }
                }
                putFieldName(field, updater.newUpdaterFieldNameIndex)
            }
        }
    }
}
