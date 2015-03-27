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

import org.jetbrains.kotlin.codegen.binding.CodegenBinding
import org.jetbrains.kotlin.codegen.state.GenerationState
import org.jetbrains.kotlin.codegen.state.JetTypeMapper
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.psi.JetClass
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

public class DefaultParameterValueSubstitutor(val state: GenerationState) {
    fun generateDefaultConstructorIfNeeded(method: CallableMethod,
                                           constructorDescriptor: ConstructorDescriptor,
                                           classBuilder: ClassBuilder,
                                           classOrObject: JetClassOrObject) {
        if (!isEmptyConstructorNeeded(constructorDescriptor, classOrObject)) {
            return
        }

        val flags = AsmUtil.getVisibilityAccessFlag(constructorDescriptor)
        val mv = classBuilder.newMethod(OtherOrigin(constructorDescriptor), flags, "<init>", "()V", null,
                                        FunctionCodegen.getThrownExceptions(constructorDescriptor, state.getTypeMapper()))

        if (state.getClassBuilderMode() == ClassBuilderMode.LIGHT_CLASSES) return

        val v = InstructionAdapter(mv)
        mv.visitCode()

        val methodOwner = method.getOwner()
        v.load(0, methodOwner) // Load this on stack

        var mask = 0
        val masks = arrayListOf<Int>()
        for (parameterDescriptor in constructorDescriptor.getValueParameters()) {
            val paramType = state.getTypeMapper().mapType(parameterDescriptor.getType())
            AsmUtil.pushDefaultValueOnStack(paramType, v)
            val i = parameterDescriptor.getIndex()
            if (i != 0 && i % Integer.SIZE == 0) {
                masks.add(mask)
                mask = 0
            }
            mask = mask or (1 shl (i % Integer.SIZE))
        }
        masks.add(mask)
        for (m in masks) {
            v.iconst(m)
        }

        // constructors with default arguments has last synthetic argument of specific type
        v.aconst(null)

        val desc = JetTypeMapper.getDefaultDescriptor(method.getAsmMethod(), false)
        v.invokespecial(methodOwner.getInternalName(), "<init>", desc, false)
        v.areturn(Type.VOID_TYPE)
        FunctionCodegen.endVisit(mv, "default constructor for " + methodOwner.getInternalName(), classOrObject)
    }

    private fun isEmptyConstructorNeeded(constructorDescriptor: ConstructorDescriptor, classOrObject: JetClassOrObject): Boolean {
        val classDescriptor = constructorDescriptor.getContainingDeclaration()

        if (classOrObject.isLocal()) return false

        if (CodegenBinding.canHaveOuter(state.getBindingContext(), classDescriptor)) return false

        if (Visibilities.isPrivate(classDescriptor.getVisibility()) || Visibilities.isPrivate(constructorDescriptor.getVisibility()))
            return false

        if (constructorDescriptor.getValueParameters().isEmpty()) return false
        if (classOrObject is JetClass && hasSecondaryConstructorsWithNoParameters(classOrObject)) return false

        return constructorDescriptor.getValueParameters().all { it.declaresDefaultValue() }
    }

    private fun hasSecondaryConstructorsWithNoParameters(klass: JetClass) =
        klass.getSecondaryConstructors().any { it.getValueParameters().isEmpty() }

}
