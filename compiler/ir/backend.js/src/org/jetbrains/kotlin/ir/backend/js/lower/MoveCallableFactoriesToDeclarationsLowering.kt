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
import org.jetbrains.kotlin.ir.backend.js.originalFileForExternalDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrRichFunctionReference
import org.jetbrains.kotlin.ir.util.addChild
import org.jetbrains.kotlin.ir.util.fileOrNull

/**
 * This lowering moves callable references implementations from the consumer-site into the declaration-site.
 *
 * A simplified pseudocode example:
 *
 * Before:
 * ```
 * // lib.kt
 * fun a() { println("Hello") }
 *
 * // consumer1.kt
 * fun main() {
 *   val callableRef = a$ref1()
 * }
 *
 * fun a$ref1(): KFunctionImpl {
 *   // <callable ref implementation>
 * }
 *
 * // consumer2.kt
 * fun main() {
 *   val callableRef = a$ref2()
 * }
 *
 * fun a$ref2(): KFunctionImpl {
 *   // <callable ref implementation>
 * }
 * ```
 *
 * After:
 * ```
 * // lib.kt
 * fun a() { println("Hello") }
 *
 * fun a$ref1(): KFunctionImpl {
 *   // <callable ref implementation>
 * }
 *
 * fun a$ref2(): KFunctionImpl {
 *   // <callable ref implementation>
 * }
 *
 * // consumer1.kt
 * fun main() {
 *   val callableRef = a$ref1()
 * }
 *
 * // consumer2.kt
 * fun main() {
 *   val callableRef = a$ref2()
 * }
 * ```
 *
 * All callable reference implementations supposed to be relocated are always moved to a file level as top-level declarations.
 * Cross-module references are not movable at this point, they remain at the consumer site.
 * Deduplication is happening next in [DeduplicateCallableReferenceFactoriesLowering].
 */
class MoveCallableFactoriesToDeclarationsLowering(private val context: JsIrBackendContext) : DeclarationTransformer {
    override fun transformFlat(declaration: IrDeclaration): List<IrDeclaration>? {
        val callableReferenceFactory = declaration as? IrSimpleFunction ?: return null
        if (callableReferenceFactory.origin != JsStatementOrigins.FACTORY_ORIGIN) return null

        val targetRichReference = callableReferenceFactory.richFunctionReference ?: return null
        val targetFunction = targetRichReference.reflectionTargetSymbol?.owner as? IrSimpleFunction ?: return null

        val sourceFile = callableReferenceFactory.fileOrNull ?: return null
        val destinationFile = targetFunction.originalFileForExternalDeclaration ?: targetFunction.fileOrNull ?: return null

        if (callableReferenceFactory.fileOrNull == destinationFile) return null
        if (sourceFile.module != destinationFile.module) return null

        destinationFile.addChild(callableReferenceFactory)
        return listOf()
    }
}

/**
 * For a callable reference factory returns it's corresponding [IrRichFunctionReference] instance.
 */
internal val IrSimpleFunction.richFunctionReference: IrRichFunctionReference?
    get() {
        val originalClass = originalCallableReferenceClass ?: return null
        return originalClass.originalCallableReference
    }