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

import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.Attribute
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.TypePath
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

open class MethodBodyVisitor(mv: MethodVisitor, private val visitAnnotationsAndAttributes: Boolean = false) : InstructionAdapter(API, mv) {

    override fun visitParameter(name: String, access: Int) {
        if (visitAnnotationsAndAttributes) super.visitParameter(name, access)
    }

    override fun visitAnnotationDefault(): AnnotationVisitor? =
            if (visitAnnotationsAndAttributes) super.visitAnnotationDefault() else null

    override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor? =
            if (visitAnnotationsAndAttributes) super.visitAnnotation(desc, visible) else null

    override fun visitTypeAnnotation(typeRef: Int, typePath: TypePath, desc: String, visible: Boolean): AnnotationVisitor? =
            if (visitAnnotationsAndAttributes) super.visitTypeAnnotation(typeRef, typePath, desc, visible) else null

    override fun visitParameterAnnotation(parameter: Int, desc: String, visible: Boolean): AnnotationVisitor? =
            if (visitAnnotationsAndAttributes) super.visitParameterAnnotation(parameter, desc, visible) else null

    override fun visitAttribute(attr: Attribute) {
        if (visitAnnotationsAndAttributes) super.visitAttribute(attr)
    }

    override fun visitCode() {
        if (visitAnnotationsAndAttributes) super.visitCode()
    }

    override fun visitMaxs(maxStack: Int, maxLocals: Int) {}

    override fun visitEnd() {}
}
