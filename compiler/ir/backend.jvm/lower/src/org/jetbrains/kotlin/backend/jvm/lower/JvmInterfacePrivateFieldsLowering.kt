/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.isJvmInterface
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrSetFieldImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.transformFlat
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

/**
 * Moves non-public static fields of interfaces into a private nested class.
 * Non-public static fields may be generated for companion block members.
 *
 * ```java
 * interface I {
 *     private static final int x = 10;
 *     private static final String y = "test";
 * }
 * ```
 *
 * becomes
 *
 * ```java
 * interface I {
 *     static { PrivateFields.x = 10; }
 *     static { PrivateFields.y = "test"; }
 *     private static class PrivateFields {
 *         public static int x;
 *         public static String y;
 *     }
 * }
 * ```
 */
internal class JvmInterfacePrivateFieldsLowering(val context: JvmBackendContext) : ClassLoweringPass, IrElementTransformerVoid() {
    override fun lower(irClass: IrClass) {
        if (!irClass.isJvmInterface) return

        var hasPrivateFields = false

        irClass.declarations.transformFlat { declaration ->
            if (declaration is IrField && declaration.isStatic && !declaration.isJvmPublic &&
                declaration.origin != JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE &&
                declaration.origin != JvmLoweredDeclarationOrigin.GENERATED_ASSERTION_ENABLED_FIELD
            ) {
                hasPrivateFields = true
                val privateFieldsClass = context.cachedDeclarations.getInterfacePrivateFieldsClass(irClass)
                privateFieldsClass.declarations.add(declaration)
                declaration.visibility = DescriptorVisibilities.PUBLIC
                declaration.isFinal = false
                declaration.parent = privateFieldsClass
                when (val initializer = declaration.extractInitializerIntoStaticAnon(context)) {
                    null -> emptyList()
                    else -> listOf(initializer)
                }
            } else {
                listOf(declaration)
            }
        }

        if (hasPrivateFields) {
            irClass.declarations.add(context.cachedDeclarations.getInterfacePrivateFieldsClass(irClass))
        }
    }
}

private val IrDeclarationWithVisibility.isJvmPublic: Boolean
    get() = visibility.delegate == Visibilities.Public || visibility.delegate == Visibilities.Internal

private fun IrField.extractInitializerIntoStaticAnon(context: JvmBackendContext): IrAnonymousInitializer? {
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
        parent = irClass
        val statement = IrSetFieldImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            symbol = irFieldSymbol,
            receiver = null,
            value = initializer.expression,
            type = context.irBuiltIns.unitType,
            origin = IrStatementOrigin.INITIALIZE_FIELD,
        )
        body = irClass.factory.createBlockBody(startOffset, endOffset).apply {
            statements.add(statement)
        }
    }
    return staticInitializer
}
