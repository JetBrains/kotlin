/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.lower.InventNamesForLocalClasses
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.utils.sanitizeName
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.isAnonymousObject

class JsInventNamesForLocalClasses(private val context: JsIrBackendContext) : InventNamesForLocalClasses() {
    override fun computeTopLevelClassName(clazz: IrClass): String = clazz.name.toString()

    override fun sanitizeNameIfNeeded(name: String): String = sanitizeName(name, withHash = false)

    override fun customizeNameInventorData(clazz: IrClass, data: NameInventorData): NameInventorData {
        if (!clazz.isAnonymousObject) return data
        val customEnclosingName = (clazz.parent as? IrFile)?.packagePartClassName?.let(::sanitizeNameIfNeeded) ?: return data
        return data.copy(enclosingName = customEnclosingName, isLocal = true)
    }

    override fun putLocalClassName(declaration: IrAttributeContainer, localClassName: String) {
        if (declaration is IrClass) {
            context.localClassNames[declaration] = localClassName
        }
    }
}
