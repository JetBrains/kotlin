/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.extensions

import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.extensions.ProjectExtensionDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.org.objectweb.asm.*

/**
 * An extension to the Kotlin/JVM compiler backend which allows to change how IR is generated into the class files.
 * It's preferable for compiler plugins to use [IrGenerationExtension] to implement IR-based logic. This extension point is more low-level.
 */
interface ClassGeneratorExtension {
    companion object : ProjectExtensionDescriptor<ClassGeneratorExtension>(
        "org.jetbrains.kotlin.classGeneratorExtension", ClassGeneratorExtension::class.java
    )

    /**
     * Override this method to decorate the [generator] that is used in the compiler backend to generate IR to bytecode.
     * [Interface delegation](https://kotlinlang.org/docs/delegation.html) can be used to avoid implementing each member manually.
     *
     * @param generator the generator used to generate the original class
     * @param declaration the IR representation of the generated class, or `null` if this class has no IR representation
     *   (for example, if it's an anonymous object copied during inlining bytecode)
     */
    fun generateClass(generator: ClassGenerator, declaration: IrClass?): ClassGenerator
}

/**
 * Similarly to ASM's [ClassWriter], provides methods that are used to generate parts of the class.
 * [newField] and [newMethod] accept an IR element, which the compiler plugin can use to implement its custom logic.
 */
interface ClassGenerator {
    fun defineClass(version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<out String>)

    fun newField(
        declaration: IrField?, access: Int, name: String, desc: String, signature: String?, value: Any?
    ): FieldVisitor

    fun newMethod(
        declaration: IrFunction?, access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?
    ): MethodVisitor

    fun newRecordComponent(name: String, desc: String, signature: String?): RecordComponentVisitor

    fun visitAnnotation(desc: String, visible: Boolean): AnnotationVisitor

    fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int)

    fun visitEnclosingMethod(owner: String, name: String?, desc: String?)

    fun visitSource(name: String, debug: String?)

    fun done(generateSmapCopyToAnnotation: Boolean)
}
