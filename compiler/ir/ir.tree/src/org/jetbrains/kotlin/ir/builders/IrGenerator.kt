/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.ir.builders

import org.jetbrains.kotlin.ir.IrBuiltIns
import org.jetbrains.kotlin.ir.declarations.IrFactory

interface IrGenerator {
    val context: IrGeneratorContext
}

interface IrGeneratorWithScope : IrGenerator {
    val scope: Scope
}

interface IrGeneratorContextInterface {
    val irBuiltIns: IrBuiltIns

    @Suppress("DEPRECATION_ERROR")
    @Deprecated("rhizomedb & noria compatibility", level = DeprecationLevel.ERROR)
    fun getIrBuiltIns() = org.jetbrains.kotlin.ir.descriptors.IrBuiltIns(irBuiltIns)
}

interface IrGeneratorContext : IrGeneratorContextInterface {
    val irFactory: IrFactory get() = irBuiltIns.irFactory
}

open class IrGeneratorContextBase(override val irBuiltIns: IrBuiltIns) : IrGeneratorContext {

    @Suppress("DEPRECATION_ERROR")
    @Deprecated("noria compatibility", level = DeprecationLevel.ERROR)
    constructor(irBuiltIns: org.jetbrains.kotlin.ir.descriptors.IrBuiltIns) : this(irBuiltIns.irBuiltIns)
}
