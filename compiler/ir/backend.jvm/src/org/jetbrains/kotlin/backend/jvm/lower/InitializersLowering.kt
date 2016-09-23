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

package org.jetbrains.kotlin.backend.jvm.lower

import org.jetbrains.kotlin.backend.jvm.codegen.getMemberOwnerKind
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockBodyImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrBlockImpl
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns


class InitializersLowering {
    fun lower(irClass: IrClass) {
        val classMemberOwnerKind = irClass.descriptor.getMemberOwnerKind()

        val staticInitializerBody = IrBlockBodyImpl(irClass.startOffset, irClass.endOffset)
        val instanceInitializerBlock = IrBlockImpl(irClass.startOffset, irClass.endOffset, irClass.descriptor.builtIns.unitType, null)

        // TODO
    }
}