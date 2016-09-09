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

package org.jetbrains.kotlin.ir.declarations

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.MODULE_SLOT
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.descriptors.IrBuiltIns
import java.lang.AssertionError

interface IrModule : IrElement {
    override val startOffset: Int get() = UNDEFINED_OFFSET
    override val endOffset: Int get() = UNDEFINED_OFFSET

    override val parent: IrElement? get() = null
    override val slot: Int get() = MODULE_SLOT
    override fun setTreeLocation(newParent: IrElement?, newSlot: Int) {
        throw AssertionError("IrModule can't have a parent element")
    }

    val descriptor: ModuleDescriptor
    val irBuiltins: IrBuiltIns
    val files: List<IrFile>
}

