/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.wasm.lower

import org.jetbrains.kotlin.backend.common.FileLoweringPass
import org.jetbrains.kotlin.backend.common.lower.createIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.backend.wasm.WasmBackendContext
import org.jetbrains.kotlin.backend.wasm.WasmSymbols
import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.backend.js.utils.associatedObject
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.ir.visitors.acceptChildrenVoid

/**
 * Adding associated objects into objects dictionary
 *
 * For code like this:
 * annotation class Key(klass: KClass<*>)
 * object OBJ
 *
 * @Key(OBJ::class)
 * class C
 *
 * add getter expression into tryGetAssociatedObject body:
 * internal fun tryGetAssociatedObject(klassId: Int, keyId: Int): Any? {
 *   ...
 *   if (C.klassId == klassId) if (Key.klassId == keyId) return OBJ
 *   ...
 *   return null
 * }
 */
class AssociatedObjectsLowering(val context: WasmBackendContext) : FileLoweringPass {
    lateinit var currentFile: IrFile

    override fun lower(irFile: IrFile) {
        currentFile = irFile
        irFile.acceptChildrenVoid(visitor)
    }

    private val visitor = object : IrElementVisitorVoid {
        override fun visitElement(element: IrElement) {
            if (element is IrClass) {
                element.acceptChildrenVoid(this)
            }
        }

        override fun visitClass(declaration: IrClass) {
            super.visitClass(declaration)

            val associatedObjects = mutableListOf<Pair<IrClass, IrClass>>()
            for (klassAnnotation in declaration.annotations) {
                val annotationClass = klassAnnotation.symbol.owner.parentClassOrNull ?: continue
                if (klassAnnotation.valueArgumentsCount != 1) continue
                if (declaration.isEffectivelyExternal()) continue
                val associatedObject = klassAnnotation.associatedObject() ?: continue
                associatedObjects += Pair(annotationClass, associatedObject)
            }
            if (associatedObjects.isNotEmpty()) {
                context.getFileContext(currentFile).classAssociatedObjects[declaration] = associatedObjects
            }
        }
    }
}
