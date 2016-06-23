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

package org.jetbrains.kotlin.codegen.inline

import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.JvmCodegenUtil
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorWithSource
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinaryPackageSourceElement
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.load.kotlin.VirtualFileKotlinClass
import org.jetbrains.kotlin.load.kotlin.incremental.components.IncrementalCache
import org.jetbrains.kotlin.resolve.source.PsiSourceElement
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedCallableMemberDescriptor
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor

val FunctionDescriptor.sourceFilePath: String
    get() {
        val source = source as PsiSourceElement
        val containingFile = source.psi?.containingFile
        return containingFile?.virtualFile?.canonicalPath!!
    }

fun FunctionDescriptor.getClassFilePath(typeMapper: KotlinTypeMapper, cache: IncrementalCache): String {
    val container = containingDeclaration as? DeclarationDescriptorWithSource
    val source = container?.source

    return when (source) {
        is KotlinJvmBinaryPackageSourceElement -> {
            val directMember = JvmCodegenUtil.getDirectMember(this)
            if (directMember !is DeserializedCallableMemberDescriptor) {
                throw AssertionError("Expected DeserializedCallableMemberDescriptor, got: $this")
            }
            val kotlinClass = source.getContainingBinaryClass(directMember) ?:
                    throw AssertionError("Descriptor $this is not found, in: $source")
            if (kotlinClass !is VirtualFileKotlinClass) {
                throw AssertionError("Expected VirtualFileKotlinClass, got $kotlinClass")
            }
            kotlinClass.file.canonicalPath!!
        }
        is KotlinJvmBinarySourceElement -> {
            val directMember = JvmCodegenUtil.getDirectMember(this)
            assert(directMember is DeserializedCallableMemberDescriptor) { "Expected DeserializedSimpleFunctionDescriptor, got: $this" }
            val kotlinClass = source.binaryClass as VirtualFileKotlinClass
            kotlinClass.file.canonicalPath!!
        }
        else -> {
            val implementationOwnerType = typeMapper.mapImplementationOwner(this)
            val className = implementationOwnerType.internalName
            cache.getClassFilePath(className)
        }
    }
}

class InlineOnlySmapSkipper(codegen: ExpressionCodegen) {

    val callLineNumber = codegen.lastLineNumber

    fun markCallSiteLineNumber(mv: MethodVisitor) {
        if (callLineNumber >= 0) {
            val label = Label()
            mv.visitLabel(label)
            mv.visitLineNumber(callLineNumber, label)
        }
    }
}