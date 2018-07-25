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

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns

interface IrGenerator {
    val context: IrGeneratorContext
}

interface IrGeneratorWithScope : IrGenerator {
    val scope: Scope
}

abstract class IrGeneratorContext {
    abstract val irBuiltIns: IrBuiltIns

    val builtIns: KotlinBuiltIns get() = irBuiltIns.builtIns
}

open class IrGeneratorContextBase(override val irBuiltIns: IrBuiltIns) : IrGeneratorContext()