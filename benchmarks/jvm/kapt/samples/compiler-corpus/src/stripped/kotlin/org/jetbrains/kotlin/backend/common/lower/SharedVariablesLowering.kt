/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.backend.common.lower

import org.jetbrains.kotlin.backend.common.BodyLoweringPass
import org.jetbrains.kotlin.backend.common.LoweringContext
import org.jetbrains.kotlin.backend.common.phaser.PhasePrerequisites
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.symbols.IrVariableSymbol
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.isInlineParameter
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.IrVisitor
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.utils.addToStdlib.runIf

/**
 * Transforms declarations and usages of variables captured in lambdas ("shared variables") to `ObjectRef`/`IntRef`/...
 *
 * For example:
 *
 *     var x = "a"
 *     run {
 *         x = "b"
 *         println(x)
 *     }
 *
 * becomes
 *
 *     var x = ObjectRef<String>()
 *     x.element = "a"
 *     run {
 *         x.element = "b"
 *         println(x.element)
 *     }
 */
@PhasePrerequisites(LateinitLowering::class)
open class SharedVariablesLowering(val context: LoweringContext, val skipRichCallables: Boolean = true) : BodyLoweringPass {
    override fun lower(irBody: IrBody, container: IrDeclaration) { TODO() }

    private inner class SharedVariablesTransformer(val irBody: IrBody, val irDeclaration: IrDeclaration) {
        private val sharedVariables = HashSet<IrVariable>()

        fun lowerSharedVariables() { TODO() }

        private fun collectSharedVariables() { TODO() }

        private fun rewriteSharedVariables() { TODO() }
    }
}
