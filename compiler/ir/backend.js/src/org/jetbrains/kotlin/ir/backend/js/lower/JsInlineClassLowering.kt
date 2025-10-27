/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.backend.common.lower.InlineClassLowering
import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.coroutines.shouldBeCompiledAsGenerator
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction

class JsInlineClassLowering(context: JsIrBackendContext) : InlineClassLowering(context) {
    /**
     * After the InlineClassLowering, the member methods are delegating to the top-level functions like this:
     *
     * ```kotlin
     * fun InlineClass_member(): T { ... }
     *
     * inline class InlineClass {
     *   fun member(): T { return InlineClass_member() }
     * }
     * ```
     *
     * For any suspension point with generators compilation
     * it's required to call suspend functions with `yield*` inside the generator context,
     * however, with saving the shouldBeCompiledAsGenerator flag we generate the wrong delegation call
     *
     * ```javascript
     * function *InlineClass_member() { ... }
     *
     * class InlineClass {
     *   // It's wrong, since called without yield*
     *   *member() { return InlineClass_member() }
     *
     *   // It's should be either like this
     *   *member() { return yield* InlineClass_member() }
     *
     *   // Or like this
     *   member() { return InlineClass_member() }
     * }
     * ```
     *
     * We're setting the shouldBeCompiledAsGenerator as false to achieve the last variant, where the member function
     * delegating to the top-level generator without a yield* statement and themselves is not a generator
     *
     * ```javascript
     * class InlineClass {
     *   member() { return InlineClass_member() }
     * }
     * ```
     */
    override fun processDelegatedInlineClassMember(declaration: IrDeclaration) {
        if (declaration is IrSimpleFunction && declaration.shouldBeCompiledAsGenerator) {
            declaration.shouldBeCompiledAsGenerator = false
        }
        super.processDelegatedInlineClassMember(declaration)
    }
}