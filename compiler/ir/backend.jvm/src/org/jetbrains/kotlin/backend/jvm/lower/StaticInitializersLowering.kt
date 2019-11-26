/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.common.CommonBackendContext
import org.jetbrains.kotlin.backend.common.ir.SetDeclarationsParentVisitor
import org.jetbrains.kotlin.backend.common.lower.InitializersLoweringBase
import org.jetbrains.kotlin.backend.jvm.JvmLoweredDeclarationOrigin
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.descriptors.WrappedSimpleFunctionDescriptor
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrSimpleFunctionSymbolImpl
import org.jetbrains.kotlin.name.Name

class StaticInitializersLowering(context: CommonBackendContext) : InitializersLoweringBase(context) {
    override fun lower(irClass: IrClass) {
        val staticDeclarations = getDeclarationsWithStaticInitializers(irClass)
        val staticInitializerStatements = staticDeclarations.mapNotNull { handleDeclaration(irClass, it) }
        if (staticInitializerStatements.isNotEmpty()) {
            createStaticInitializationMethod(irClass, staticInitializerStatements)

            val anonymousInitializers = staticDeclarations.filterTo(hashSetOf()) { it is IrAnonymousInitializer }
            irClass.declarations.removeAll(anonymousInitializers)
        }
    }

    private fun getDeclarationsWithStaticInitializers(irClass: IrClass): List<IrDeclaration> =
        // Hardcoded order of initializers
        (irClass.declarations.filter { it is IrField && it.origin == IrDeclarationOrigin.FIELD_FOR_ENUM_ENTRY } +
                irClass.declarations.filter { it is IrField && it.origin == IrDeclarationOrigin.FIELD_FOR_ENUM_VALUES } +
                irClass.declarations.filter { it is IrField && it.origin == IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE } +
                irClass.declarations.filter {
                    (it is IrField && it.isStatic && it.origin !in listOf(
                        IrDeclarationOrigin.FIELD_FOR_ENUM_ENTRY,
                        IrDeclarationOrigin.FIELD_FOR_ENUM_VALUES,
                        IrDeclarationOrigin.FIELD_FOR_OBJECT_INSTANCE
                    )) || (it is IrAnonymousInitializer && it.isStatic)
                })

    private fun createStaticInitializationMethod(irClass: IrClass, staticInitializerStatements: List<IrStatement>) {
        // TODO: mark as synthesized
        val staticInitializerDescriptor = WrappedSimpleFunctionDescriptor()
        val staticInitializer = IrFunctionImpl(
            irClass.startOffset,
            irClass.endOffset,
            JvmLoweredDeclarationOrigin.CLASS_STATIC_INITIALIZER,
            IrSimpleFunctionSymbolImpl(staticInitializerDescriptor),
            clinitName,
            Visibilities.PUBLIC,
            Modality.FINAL,
            returnType = context.irBuiltIns.unitType,
            isInline = false,
            isExternal = false,
            isTailrec = false,
            isSuspend = false,
            isExpect = false,
            isFakeOverride = false,
            isOperator = false
        ).apply {
            staticInitializerDescriptor.bind(this)
            body = IrBlockBodyImpl(irClass.startOffset, irClass.endOffset,
                                   staticInitializerStatements.map { it.copy(irClass) })
            accept(SetDeclarationsParentVisitor, this)
            // Should come after SetDeclarationParentVisitor, because it sets staticInitializer's own parent to itself.
            parent = irClass
        }
        irClass.declarations.add(staticInitializer)
    }

    companion object {
        val clinitName = Name.special("<clinit>")
    }
}
