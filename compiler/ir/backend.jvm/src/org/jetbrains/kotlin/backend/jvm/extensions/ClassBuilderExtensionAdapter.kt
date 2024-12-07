/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.extensions

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.jvm.ir.psiElement
import org.jetbrains.kotlin.codegen.ClassBuilder
import org.jetbrains.kotlin.codegen.ClassBuilderFactory
import org.jetbrains.kotlin.codegen.DelegatingClassBuilder
import org.jetbrains.kotlin.codegen.DelegatingClassBuilderFactory
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOrigin
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.FieldVisitor
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.RecordComponentVisitor

// Loads ClassGeneratorExtension implementations and converts them to deprecated ClassBuilderInterceptorExtension implementations,
// so that GenerationState (which is backend-agnostic) can apply them during class generation.
@Suppress("unused") // Used reflectively in GenerationState.
internal object ClassBuilderExtensionAdapter {
    @JvmStatic
    @Suppress("DEPRECATION_ERROR")
    fun getExtensions(project: Project): List<org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension> =
        ClassGeneratorExtension.getInstances(project).map(::ExtensionAdapter)
}

@Suppress("DEPRECATION_ERROR")
private class ExtensionAdapter(private val extension: ClassGeneratorExtension) :
    org.jetbrains.kotlin.codegen.extensions.ClassBuilderInterceptorExtension {
    override fun interceptClassBuilderFactory(
        interceptedFactory: ClassBuilderFactory,
        bindingContext: BindingContext,
        diagnostics: DiagnosticSink,
    ): ClassBuilderFactory = object : DelegatingClassBuilderFactory(interceptedFactory) {
        override fun newClassBuilder(origin: JvmDeclarationOrigin): DelegatingClassBuilder {
            val classBuilder = interceptedFactory.newClassBuilder(origin)
            val irClass = origin.unwrapOrigin<IrClass>()
            return DelegatingClassBuilderAdapter(
                extension.generateClass(
                    ClassGeneratorAdapter(irClass, classBuilder),
                    irClass
                ),
                classBuilder
            )
        }
    }
}

private class ClassGeneratorAdapter(val irClass: IrClass?, val builder: ClassBuilder) : ClassGenerator {
    override fun defineClass(
        version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<out String>
    ) {
        builder.defineClass(irClass?.psiElement, version, access, name, signature, superName, interfaces)
    }

    override fun newField(
        declaration: IrField?, access: Int, name: String, desc: String, signature: String?, value: Any?
    ): FieldVisitor =
        builder.newField(declaration.wrapToOrigin(), access, name, desc, signature, value)

    override fun newMethod(
        declaration: IrFunction?, access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?
    ): MethodVisitor =
        builder.newMethod(declaration.wrapToOrigin(), access, name, desc, signature, exceptions)

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

private class DelegatingClassBuilderAdapter(
    private val generator: ClassGenerator,
    private val originalClassBuilder: ClassBuilder,
) : DelegatingClassBuilder() {
    override fun getDelegate(): ClassBuilder = originalClassBuilder

    override fun defineClass(
        origin: PsiElement?, version: Int, access: Int, name: String, signature: String?, superName: String, interfaces: Array<out String>
    ) {
        generator.defineClass(version, access, name, signature, superName, interfaces)
    }

    override fun newField(
        origin: JvmDeclarationOrigin, access: Int, name: String, desc: String, signature: String?, value: Any?
    ): FieldVisitor =
        generator.newField(origin.unwrapOrigin(), access, name, desc, signature, value)

    override fun newMethod(
        origin: JvmDeclarationOrigin, access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?
    ): MethodVisitor =
        generator.newMethod(origin.unwrapOrigin(), access, name, desc, signature, exceptions)

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

private inline fun <reified T : IrDeclaration> JvmDeclarationOrigin.unwrapOrigin(): T? =
    (this as? JvmIrDeclarationOrigin)?.declaration as? T

private fun IrDeclaration?.wrapToOrigin(): JvmDeclarationOrigin =
    this?.descriptorOrigin ?: JvmDeclarationOrigin.NO_ORIGIN
