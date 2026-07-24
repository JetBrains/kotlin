/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.extensions

import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.DelegatingClassBuilder
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.FieldVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.RecordComponentVisitor

internal class ClassGeneratorAdapter(val builder: ClassBuilder) : ClassGenerator {
    override fun defineClass(
        version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<out String>,
    ) {
        builder.defineClass(version, access, name, signature, superName, interfaces)
    }

    override fun newField(declaration: IrField?, access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor =
        builder.newField(declaration, access, name, desc, signature, value)

    override fun newMethod(
        declaration: IrFunction?, access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?,
    ): MethodVisitor =
        builder.newMethod(declaration, access, name, desc, signature, exceptions)

    override fun newRecordComponent(name: String, desc: String, signature: String?): RecordComponentVisitor =
        builder.newRecordComponent(name, desc, signature)

    override fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor =
        builder.newAnnotation(desc, visible)

    override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
        builder.visitInnerClass(name, outerName, innerName, access)
    }

    override fun visitEnclosingMethod(owner: String, name: String?, desc: String?) {
        builder.visitOuterClass(owner, name, desc)
    }

    override fun visitSource(name: String, debug: String?) {
        builder.visitSource(name, debug)
    }

    override fun done(generateSmapCopyToAnnotation: Boolean) {
        builder.done(generateSmapCopyToAnnotation)
    }
}

internal class DelegatingClassBuilderAdapter(
    private val generator: ClassGenerator,
    private val originalClassBuilder: ClassBuilder,
) : DelegatingClassBuilder() {
    override fun getDelegate(): ClassBuilder = originalClassBuilder

    override fun defineClass(
        version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<out String>,
    ) {
        generator.defineClass(version, access, name, signature, superName, interfaces)
    }

    override fun newField(origin: IrField?, access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor =
        generator.newField(origin, access, name, desc, signature, value)

    override fun newMethod(
        origin: IrFunction?, access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?,
    ): MethodVisitor =
        generator.newMethod(origin, access, name, desc, signature, exceptions)

    override fun newRecordComponent(name: String, desc: String, signature: String?): RecordComponentVisitor =
        generator.newRecordComponent(name, desc, signature)

    override fun newAnnotation(desc: String, visible: Boolean): AnnotationVisitor =
        generator.visitAnnotation(desc, visible)

    override fun visitOuterClass(owner: String, name: String?, desc: String?) {
        generator.visitEnclosingMethod(owner, name, desc)
    }

    override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
        generator.visitInnerClass(name, outerName, innerName, access)
    }

    override fun visitSource(name: String, debug: String?) {
        generator.visitSource(name, debug)
    }

    override fun done(generateSmapCopyToAnnotation: Boolean) {
        generator.done(generateSmapCopyToAnnotation)
    }
}
