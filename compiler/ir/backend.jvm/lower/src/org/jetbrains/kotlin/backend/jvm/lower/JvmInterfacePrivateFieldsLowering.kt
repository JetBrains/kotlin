/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.isJvmInterface
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.org.objectweb.asm.Opcodes

/**
 * Moves non-public static fields of interfaces into a private nested class.
 * Non-public static fields may be generated for companion block members.
 *
 * ```java
 * interface I {
 *     private static final int x = 10;
 *     public int getX() { return x; }
 *     private static final String y = "test";
 *     public int getY() { return y; }
 * }
 * ```
 *
 * becomes
 *
 * ```java
 * interface I {
 *     static { PrivateFields.x = 10; }
 *     public int getX() { return PrivateFields.x; }
 *     static { PrivateFields.y = "test"; }
 *     public int getY() { return PrivateFields.y; }
 *     private static class PrivateFields {
 *         static int x;
 *         static String y;
 *     }
 * }
 * ```
 */
@PhasePrerequisites(JvmPropertiesLowering::class)
internal class JvmInterfacePrivateFieldsLowering(val context: JvmBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        if (!irClass.isJvmInterface) return

        var hasNonPublicFields = false

        irClass.declarations.transformFlat { declaration ->
            if (declaration is IrField && declaration.isStatic && !declaration.isJvmPublic &&
                declaration.origin != JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE &&
                declaration.origin != JvmLoweredDeclarationOrigin.GENERATED_ASSERTION_ENABLED_FIELD
            ) {
                hasNonPublicFields = true
                val privateFieldsClass = context.cachedDeclarations.getInterfacePrivateFieldsClass(irClass)
                privateFieldsClass.declarations.add(declaration)
                declaration.visibility = DescriptorVisibilities.LOCAL
                declaration.isFinal = false
                declaration.parent = privateFieldsClass
                listOfNotNull(declaration.extractInitializerIntoStaticAnonymousInitializer(context, irClass))
            } else {
                null
            }
        }

        if (hasNonPublicFields) {
            irClass.declarations.add(context.cachedDeclarations.getInterfacePrivateFieldsClass(irClass))
        }
    }
}

private val IrDeclarationWithVisibility.isJvmPublic: Boolean
    get() = AsmUtil.getVisibilityAccessFlag(visibility.delegate) == Opcodes.ACC_PUBLIC

private fun IrField.extractInitializerIntoStaticAnonymousInitializer(
    context: JvmBackendContext,
    interfaceClass: IrClass
): IrAnonymousInitializer? {
    val initializer = this.initializer ?: return null
    this.initializer = null
    val irClass = this.parentAsClass
    val irFieldSymbol = this.symbol
    val staticInitializer = irClass.factory.createAnonymousInitializer(
        startOffset = initializer.startOffset,
        endOffset = initializer.endOffset,
        origin = JvmLoweredDeclarationOrigin.INTERFACE_PRIVATE_FIELDS_CLASS,
        symbol = IrAnonymousInitializerSymbolImpl(irClass.symbol),
        isStatic = true,
    ).apply {
        parent = interfaceClass
        val statement = IrSetFieldImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            symbol = irFieldSymbol,
            receiver = null,
            value = initializer.expression,
            type = context.irBuiltIns.unitType,
            origin = IrStatementOrigin.INITIALIZE_FIELD,
        )
        body = irClass.factory.createBlockBody(startOffset, endOffset, listOf(statement))
    }
    return staticInitializer
}
