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

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.codegen.AsmUtil.isStaticMethod
import org.jetbrains.kotlin.codegen.FunctionCodegen.createFrameMap
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.org.objectweb.asm.Opcodes.ACC_STATIC
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class FunctionCodegen(val irFunction: IrFunction, val classCodegen: ClassCodegen) {

    fun generate() {
        val signature = classCodegen.typeMapper.mapSignatureWithGeneric(irFunction.descriptor, OwnerKind.IMPLEMENTATION)
        val isStatic = isStaticMethod(
                if (classCodegen.descriptor.isFileDescriptor) OwnerKind.PACKAGE else OwnerKind.IMPLEMENTATION,
                irFunction.descriptor
        )
        val frameMap = createFrameMap(
                classCodegen.state, irFunction.descriptor, signature,
                isStatic
        )

        val methodVisitor = classCodegen.visitor.newMethod(irFunction.OtherOrigin,
                                                           irFunction.descriptor.calculateCommonFlags().or(if (isStatic) ACC_STATIC else 0),
                                                           signature.asmMethod.name, signature.asmMethod.descriptor,
                                                           signature.genericsSignature, null/*TODO support exception*/)

        ExpressionCodegen(irFunction, frameMap, InstructionAdapter(methodVisitor), classCodegen).generate()
    }
}
