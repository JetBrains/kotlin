/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen

import org.jetbrains.jet.lang.descriptors.FunctionDescriptor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.jet.codegen.state.GenerationState
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.jet.lang.descriptors.ClassDescriptor
import org.jetbrains.jet.lang.resolve.java.diagnostics.JvmDeclarationOrigin
import org.jetbrains.jet.lang.resolve.java.diagnostics.Synthetic

class PlatformStaticGenerator(
        val descriptor: FunctionDescriptor,
        val declarationOrigin: JvmDeclarationOrigin,
        val state: GenerationState
) : Function2<ImplementationBodyCodegen, ClassBuilder, Unit> {

    override fun invoke(p1: ImplementationBodyCodegen, p2: ClassBuilder) {
        val typeMapper = state.getTypeMapper()
        val callable = typeMapper.mapToCallableMethod(descriptor, false, p1.getContext())
        val asmMethod = callable.getAsmMethod()
        val methodVisitor = p2.newMethod(
                Synthetic(declarationOrigin.element, descriptor),
                Opcodes.ACC_STATIC or AsmUtil.getMethodAsmFlags(descriptor, OwnerKind.IMPLEMENTATION),
                asmMethod.getName()!!,
                asmMethod.getDescriptor()!!,
                null, null)

        AnnotationCodegen.forMethod(methodVisitor, typeMapper)!!.genAnnotations(descriptor, asmMethod.getReturnType())

        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            methodVisitor.visitCode();
            val iv = InstructionAdapter(methodVisitor)
            val classDescriptor = descriptor.getContainingDeclaration() as ClassDescriptor
            val singletonValue = StackValue.singleton(classDescriptor, typeMapper)!!
            singletonValue.put(singletonValue.type, iv);
            var index = 0;
            for (paramType in asmMethod.getArgumentTypes()) {
                iv.load(index, paramType);
                index += paramType.getSize();
            }
            callable.invokeWithoutAssertions(iv)
            iv.areturn(asmMethod.getReturnType());
            methodVisitor.visitEnd();
        }
    }
}