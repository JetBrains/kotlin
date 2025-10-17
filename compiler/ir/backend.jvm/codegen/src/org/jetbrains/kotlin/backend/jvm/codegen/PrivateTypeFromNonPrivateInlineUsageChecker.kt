/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.backend.jvm.JvmBackendContext
import org.jetbrains.kotlin.backend.jvm.JvmBackendErrors
import org.jetbrains.kotlin.backend.jvm.ir.fileParent
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.overrides.isEffectivelyPrivate
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.MethodNode

internal class PrivateTypeFromNonPrivateInlineUsageChecker(
    private val context: JvmBackendContext,
) : MethodVisitor(Opcodes.API_VERSION) {
    private val result: MutableSet<ClassId> = mutableSetOf()
    private fun findPrivateClassUsages(node: MethodNode): Collection<ClassId> = result.also { node.accept(this) }

    override fun visitLdcInsn(value: Any?) {
        if (value is Type) {
            checkType(value)
        }
    }

    override fun visitTypeInsn(opcode: Int, type: String) {
        checkType(Type.getObjectType(type))
    }

    override fun visitMethodInsn(opcode: Int, owner: String, name: String, descriptor: String, isInterface: Boolean) {
        checkType(Type.getObjectType(owner))
        checkType(Type.getReturnType(descriptor))
        for (type in Type.getArgumentTypes(descriptor)) {
            checkType(type)
        }
    }

    override fun visitFieldInsn(opcode: Int, owner: String, name: String, descriptor: String) {
        checkType(Type.getObjectType(owner))
        checkType(Type.getType(descriptor))
    }

    override fun visitTryCatchBlock(start: Label, end: Label, handler: Label, type: String?) {
        if (type != null) {
            checkType(Type.getObjectType(type))
        }
    }

    override fun visitLocalVariable(name: String, descriptor: String, signature: String?, start: Label, end: Label, index: Int) {
        checkType(Type.getType(descriptor))
    }

    private fun checkType(type: Type) {
        // `JvmBackendClassResolver.resolveToClassDescriptors` doesn't work for classes with '$' in the name (where '$' is a part of the
        // name, not the outer-inner separator), but those are so rare, especially in Kotlin sources, that we won't care about them here.
        for (classDescriptor in context.state.jvmBackendClassResolver.resolveToClassDescriptors(type)) {
            if (DescriptorVisibilities.isPrivate(classDescriptor.visibility)) {
                result.add(classDescriptor.classId ?: ClassId(FqName.ROOT, SpecialNames.NO_NAME_PROVIDED))
            }
        }
    }

    companion object {
        fun check(caller: IrFunction, call: IrExpression, callee: IrFunction, node: MethodNode, context: JvmBackendContext) {
            if (caller.isInline && !caller.isEffectivelyPrivate() && callee.isEffectivelyPrivate()) {
                val privateClassIds = PrivateTypeFromNonPrivateInlineUsageChecker(context).findPrivateClassUsages(node)
                for (classId in privateClassIds) {
                    context.ktDiagnosticReporter.at(call, caller.fileParent)
                        .report(JvmBackendErrors.PRIVATE_TYPE_USED_IN_NON_PRIVATE_INLINE_FUNCTION, classId)
                }
            }
        }
    }
}
