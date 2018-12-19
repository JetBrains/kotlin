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

import org.jetbrains.org.objectweb.asm.*
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

open class SkipMaxAndEndVisitor(mv: MethodVisitor) : InstructionAdapter(Opcodes.API_VERSION, mv) {
    override fun visitMaxs(maxStack: Int, maxLocals: Int) {}

    override fun visitEnd() {}
}

open class MethodBodyVisitor(mv: MethodVisitor) : MethodVisitor(Opcodes.API_VERSION, mv) {

    @Suppress("NOTHING_TO_OVERRIDE")
    override fun visitAnnotableParameterCount(parameterCount: Int, visible: Boolean) {
    }

    override fun visitParameter(name: String, access: Int) {}

    override fun visitAnnotationDefault(): AnnotationVisitor? = null

    override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? = null

    override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath, desc: String, visible: Boolean): AnnotationVisitor? = null

    override fun visitParameterAnnotation(parameter: Int, desc: String, visible: Boolean): AnnotationVisitor? = null

    override fun visitAttribute(attr: Attribute) {}

    override fun visitCode() {}

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {}

    override fun visitEnd() {}
}
