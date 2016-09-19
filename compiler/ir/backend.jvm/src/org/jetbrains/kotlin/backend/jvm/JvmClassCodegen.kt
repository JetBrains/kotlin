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

package org.jetbrains.kotlin.backend.jvm

import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ImplementationBodyCodegen
import org.jetbrains.kotlin.codegen.MemberCodegen.badDescriptor
import org.jetbrains.kotlin.codegen.OwnerKind
import org.jetbrains.kotlin.codegen.SuperClassInfo
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.kotlin.resolve.jvm.diagnostics.OtherOrigin
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.types.ErrorUtils
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class JvmClassCodegen private constructor(val irClass: IrClass, val context: JvmBackendContext) {

    val state = context.state

    val typeMapper = context.state.typeMapper

    val descriptor = irClass.descriptor

    val type = typeMapper.mapType(descriptor)

    val visitor: ClassBuilder

    init {
        val element = (irClass.descriptor.source as PsiSourceElement).psi!!
        visitor = state.factory.newVisitor(irClass.OtherOrigin, type, element.containingFile)
    }

    fun generate() {
        val superClassInfo = SuperClassInfo.getSuperClassInfo(descriptor, typeMapper)
        val signature = ImplementationBodyCodegen.signature(descriptor, type, superClassInfo, typeMapper)

        visitor.defineClass(irClass.OtherOrigin.element, state.classFileVersion,
                            descriptor.calculateClassFlags(),
                            signature.name,
                            signature.javaGenericSignature,
                            signature.superclassName,
                            signature.interfaces.toTypedArray()
        )

        irClass.declarations.forEach {
            generateDeclaration(it)
        }

        visitor.done()
    }

    companion object {
        fun generate(irClass: IrClass, context: JvmBackendContext) {
            val descriptor = irClass.descriptor
            val state = context.state

            if (ErrorUtils.isError(descriptor)) {
                badDescriptor(descriptor, state.classBuilderMode)
                return
            }

            if (descriptor.name == SpecialNames.NO_NAME_PROVIDED) {
                badDescriptor(descriptor, state.classBuilderMode)
            }

            JvmClassCodegen(irClass, context).generate()
        }
    }

    fun generateDeclaration(declaration: IrDeclaration) {
        when (declaration) {
            is IrProperty -> {
                declaration.backingField?.let {
                    generateField(it)
                }

                declaration.getter?.let {
                    generateMethod(it)
                }

                declaration.setter?.let {
                    generateMethod(it)
                }
            }
            is IrFunction -> {
                generateMethod(declaration)
            }
            else -> throw RuntimeException("Unsupported declaration $declaration")
        }
    }


    fun generateField(field: IrField) {
        val fieldType = typeMapper.mapType(field.descriptor)
        val fieldSignature = typeMapper.mapFieldSignature(field.descriptor.type, field.descriptor)
        visitor.newField(field.OtherOrigin, field.descriptor.calculateCommonFlags(), field.descriptor.name.asString(), fieldType.descriptor,
                         fieldSignature, null/*TODO support default values*/)
    }

    fun generateMethod(method: IrFunction) {
        FunctionCodegen(method, this).generate()
    }

}

fun ClassDescriptor.calculateClassFlags(): Int {
    return Opcodes.ACC_SUPER.or(calculateCommonFlags()).or(if (DescriptorUtils.isInterface(this)) Opcodes.ACC_INTERFACE else 0)
}

fun MemberDescriptor.calculateCommonFlags(): Int {
    var flags = 0
    if (Visibilities.isPrivate(visibility)) {
        flags = flags.or(Opcodes.ACC_PRIVATE)
    }
    else if (visibility == Visibilities.PUBLIC || visibility == Visibilities.INTERNAL) {
        flags = flags.or(Opcodes.ACC_PUBLIC)
    }
    else if (visibility == Visibilities.PROTECTED) {
        flags = flags.or(Opcodes.ACC_PROTECTED)
    }
    else {
        throw RuntimeException("Unsupported visibility $visibility for descriptor $this")
    }

    when (modality) {
        Modality.ABSTRACT -> {
            flags = flags.or(Opcodes.ACC_ABSTRACT)
        }
        Modality.FINAL -> {
            if (this !is ConstructorDescriptor) {
                flags = flags.or(Opcodes.ACC_FINAL)
            }
        }
        Modality.OPEN -> {
            assert(!Visibilities.isPrivate(visibility))
        }
        else -> throw RuntimeException("Unsupported modality $modality for descriptor $this")
    }
    return flags
}

val IrClass.OtherOrigin: JvmDeclarationOrigin
    get() = OtherOrigin((this.descriptor.source as PsiSourceElement).psi!!, this.descriptor)

val IrField.OtherOrigin: JvmDeclarationOrigin
    get() = OtherOrigin((this.descriptor.source as PsiSourceElement).psi!!, this.descriptor)

val IrFunction.OtherOrigin: JvmDeclarationOrigin
    get() = OtherOrigin((this.descriptor.source as PsiSourceElement).psi!!, this.descriptor)