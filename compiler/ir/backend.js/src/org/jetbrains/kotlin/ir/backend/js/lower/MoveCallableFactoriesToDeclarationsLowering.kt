/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.DeclarationTransformer
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.JsStatementOrigins
import org.jetbrains.kotlin.ir.backend.js.originalCallableReferenceClass
import org.jetbrains.kotlin.ir.backend.js.originalCallableReference
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.isEffectivelyExternal

/**
 * This lowering moves callable references implementations from the consumer-site into the declaration-site.
 *
 * A simplified pseudocode example:
 *
 * Before:
 * ```
 * // lib.kt
 *   function a() { println("Hello") }
 *
 * // consumer1.kt
 *   fun main() {
 *     val callableRef = a$ref1()
 *   }
 *
 *   fun a$ref1(): KFunctionImpl {
 *     // <callable ref implementation>
 *   }
 *
 * // consumer2.kt
 *   fun main() {
 *     val callableRef = a$ref2()
 *   }
 *
 *   fun a$ref2(): KFunctionImpl {
 *     // <callable ref implementation>
 *   }
 * ```
 *
 * After:
 * ```
 * // lib.kt
 *   function a() { println("Hello") }
 *
 *   fun a$ref1(): KFunctionImpl {
 *     // <callable ref implementation>
 *   }
 *
 *   fun a$ref2(): KFunctionImpl {
 *     // <callable ref implementation>
 *   }
 *
 * // consumer1.kt
 *   fun main() {
 *     val callableRef = a$ref1()
 *   }
 *
 * // consumer2.kt
 *   fun main() {
 *     val callableRef = a$ref2()
 *   }
 * ```
 *
 * All callable reference implementations supposed to be relocated are always moved to a file level as top-level declarations.
 *
 * Cross-module references are not movable at this point, they remain at the consumer site.
 *
 * Deduplication is happening next in [DeduplicateCallableFactoriesLowering].
 */
class MoveCallableFactoriesToDeclarationsLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        if (declaration !is IrSimpleFunction) return null
        if (declaration.origin != JsStatementOrigins.FACTORY_ORIGIN) return null

        val originalReference = declaration.richFunctionReference ?: return null
        val originalFunction = originalReference.reflectionTargetSymbol as? IrSimpleFunction ?: return null
        val destinationFile = getOriginalFile(originalFunction) ?: return null
        if (declaration.getPackageFragment() == destinationFile) return null

        destinationFile.addChild(declaration)
        return listOf()
    }

    private fun getOriginalFile(declaration: IrSimpleFunction): IrFile? {
        return when {
            declaration.isEffectivelyExternal() -> {
                val shadowFile = declaration.getPackageFragment() as? IrFile
                shadowFile?.module?.files?.firstOrNull { it.fileEntry == shadowFile.fileEntry }
            }
            else -> declaration.getPackageFragment() as? IrFile
        }
    }
}

internal val IrSimpleFunction.richFunctionReference: IrRichFunctionReference?
    get() {
        val originalClass = originalCallableReferenceClass ?: return null
        return originalClass.originalCallableReference
    }