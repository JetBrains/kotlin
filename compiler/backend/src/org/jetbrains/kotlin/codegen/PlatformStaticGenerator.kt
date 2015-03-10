/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.Synthetic
import org.jetbrains.kotlin.descriptors.CallableMemberDescriptor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.kotlin.resolve.jvm.jvmSignature.JvmMethodSignature
import org.jetbrains.kotlin.codegen.context.MethodContext
import org.jetbrains.kotlin.psi.JetElement
import org.jetbrains.kotlin.backend.common.CodegenUtil
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import kotlin.platform.platformStatic

class PlatformStaticGenerator(
        val descriptor: FunctionDescriptor,
        val declarationOrigin: JvmDeclarationOrigin,
        val state: GenerationState
) : Function2<ImplementationBodyCodegen, ClassBuilder, Unit> {

    override fun invoke(codegen: ImplementationBodyCodegen, classBuilder: ClassBuilder) {
        val staticFunctionDescriptor = createStaticFunctionDescriptor(descriptor)

        codegen.functionCodegen.generateMethod(
                Synthetic(declarationOrigin.element, staticFunctionDescriptor),
                staticFunctionDescriptor,
                object : FunctionGenerationStrategy() {
                    override fun generateBody(
                            mv: MethodVisitor,
                            frameMap: FrameMap,
                            signature: JvmMethodSignature,
                            context: MethodContext,
                            parentCodegen: MemberCodegen<out JetElement>
                    ) {
                        val typeMapper = parentCodegen.typeMapper

                        val iv = InstructionAdapter(mv)
                        val classDescriptor = descriptor.getContainingDeclaration() as ClassDescriptor
                        val singletonValue = StackValue.singleton(classDescriptor, typeMapper)
                        singletonValue.put(singletonValue.type, iv);
                        var index = 0;
                        val asmMethod = signature.getAsmMethod()
                        for (paramType in asmMethod.getArgumentTypes()) {
                            iv.load(index, paramType);
                            index += paramType.getSize();
                        }

                        val syntheticOrOriginalMethod = typeMapper.mapToCallableMethod(
                                codegen.getContext().accessibleFunctionDescriptor(descriptor),
                                false,
                                codegen.getContext()
                        )
                        syntheticOrOriginalMethod.invokeWithoutAssertions(iv)
                        iv.areturn(asmMethod.getReturnType());
                    }
                }
        )
    }

    default object {
        [platformStatic]
        public fun createStaticFunctionDescriptor(descriptor: FunctionDescriptor): FunctionDescriptor {
            val memberDescriptor = if (descriptor is PropertyAccessorDescriptor) descriptor.getCorrespondingProperty() else descriptor
            val copies = CodegenUtil.copyFunctions(
                    memberDescriptor,
                    memberDescriptor,
                    descriptor.getContainingDeclaration().getContainingDeclaration(),
                    descriptor.getModality(),
                    descriptor.getVisibility(),
                    CallableMemberDescriptor.Kind.SYNTHESIZED,
                    false
            )
            val staticFunctionDescriptor = copies[descriptor]!!
            return staticFunctionDescriptor
        }
    }
}
