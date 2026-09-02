/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.ClassLoweringPass
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.backend.jvm.ir.createJvmIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.isJvmInterface
import org.jetbrains.kotlin.codegen.AsmUtil
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrAnonymousInitializer
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithVisibility
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.createBlockBody
import org.jetbrains.kotlin.ir.symbols.impl.IrAnonymousInitializerSymbolImpl
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
 *     // <companion object>
 *     private static final String y = "test";
 *     public String getY() { return y; }
 * }
 * ```
 *
 * becomes
 *
 * ```java
 * interface I {
 *     private static class PrivateFields1 {
 *         static int x = 10;
 *     }
 *     static { PrivateFields1.syntheticInitTrigger(); }
 *     public int getX() { return PrivateFields.x; }
 *     // <companion object>
 *     private static class PrivateFields2 {
 *         static String y = "test";
 *     }
 *     static { PrivateFields2.syntheticInitTrigger(); }
 *     public String getY() { return PrivateFields2.y; }
 * }
 * ```
 */
@PhasePrerequisites(JvmPropertiesLowering::class)
internal class JvmInterfacePrivateFieldsLowering(val context: JvmBackendContext) : ClassLoweringPass {
    override fun lower(irClass: IrClass) {
        if (!irClass.isJvmInterface) return

        var privateFields1Added = false
        var privateFields2Added = false
        var companionObjectSeen = false

        irClass.declarations.transformFlat { declaration ->
            if (declaration is IrField && declaration.isStatic && !declaration.isJvmPublic &&
                declaration.origin != JvmLoweredDeclarationOrigin.GENERATED_PROPERTY_REFERENCE &&
                declaration.origin != JvmLoweredDeclarationOrigin.GENERATED_ASSERTION_ENABLED_FIELD
            ) {
                val newDeclarations = mutableListOf<IrDeclaration>()
                val privateFieldsClass = when (companionObjectSeen) {
                    false -> {
                        context.cachedDeclarations.getInterfacePrivateFields1Class(irClass).also {
                            if (!privateFields1Added) {
                                privateFields1Added = true;
                                newDeclarations.add(it)
                                newDeclarations.add(buildSyntheticInitTriggerInitializer(context, irClass, it))
                            }
                        }
                    }
                    true -> {
                        context.cachedDeclarations.getInterfacePrivateFields2Class(irClass).also {
                            if (!privateFields2Added) {
                                privateFields2Added = true;
                                newDeclarations.add(it)
                                newDeclarations.add(buildSyntheticInitTriggerInitializer(context, irClass, it))
                            }
                        }
                    }
                }
                privateFieldsClass.declarations.add(declaration)
                declaration.visibility = DescriptorVisibilities.LOCAL
                declaration.parent = privateFieldsClass
                buildDelegateMethodIfNeeded(declaration, context, irClass)?.let { newDeclarations.add(it) }
                newDeclarations
            } else {
                if (declaration is IrClass && declaration.isCompanion) companionObjectSeen = true
                null
            }
        }
    }
}

private val IrDeclarationWithVisibility.isJvmPublic: Boolean
    get() = AsmUtil.getVisibilityAccessFlag(visibility.delegate) == Opcodes.ACC_PUBLIC

private fun buildDelegateMethodIfNeeded(
    field: IrField,
    context: JvmBackendContext,
    interfaceClass: IrClass
): IrFunction? {
    if (field.origin != IrDeclarationOrigin.PROPERTY_DELEGATE) return null

    return interfaceClass.factory.buildFun {
        name = field.name
        origin = field.origin
        returnType = field.type
        visibility = DescriptorVisibilities.PRIVATE
    }.apply {
        parent = interfaceClass
        metadata = field.metadata
        body = context.createJvmIrBuilder(symbol).run {
            irExprBody(
                irGetField(null, field)
            )
        }
    }
}

private fun buildSyntheticInitTriggerInitializer(
    context: JvmBackendContext,
    interfaceClass: IrClass,
    privateFields: IrClass
): IrAnonymousInitializer {
    return interfaceClass.factory.createAnonymousInitializer(
        startOffset = UNDEFINED_OFFSET,
        endOffset = UNDEFINED_OFFSET,
        origin = JvmLoweredDeclarationOrigin.INTERFACE_PRIVATE_FIELDS_CLASS,
        symbol = IrAnonymousInitializerSymbolImpl(interfaceClass.symbol),
        isStatic = true,
    ).apply {
        parent = interfaceClass
        val callToPrivateFieldsInitTrigger = context.createJvmIrBuilder(symbol).irCall(context.cachedDeclarations.getSyntheticClassInitTrigger(privateFields))
        body = interfaceClass.factory.createBlockBody(UNDEFINED_OFFSET, UNDEFINED_OFFSET, listOf(callToPrivateFieldsInitTrigger))
    }
}
