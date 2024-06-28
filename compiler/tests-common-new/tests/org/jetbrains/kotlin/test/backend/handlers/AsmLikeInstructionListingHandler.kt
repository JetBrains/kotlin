/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.backend.handlers

import org.jetbrains.kotlin.codegen.DefaultParameterValueSubstitutor
import org.jetbrains.kotlin.codegen.getClassFiles
import org.jetbrains.kotlin.test.directives.AsmLikeInstructionListingDirectives
import org.jetbrains.kotlin.test.directives.AsmLikeInstructionListingDirectives.CHECK_ASM_LIKE_INSTRUCTIONS
import org.jetbrains.kotlin.test.directives.AsmLikeInstructionListingDirectives.CURIOUS_ABOUT
import org.jetbrains.kotlin.test.directives.AsmLikeInstructionListingDirectives.FIR_DIFFERENCE
import org.jetbrains.kotlin.test.directives.AsmLikeInstructionListingDirectives.INLINE_SCOPES_DIFFERENCE
import org.jetbrains.kotlin.test.directives.AsmLikeInstructionListingDirectives.LOCAL_VARIABLE_TABLE
import org.jetbrains.kotlin.test.directives.AsmLikeInstructionListingDirectives.RENDER_ANNOTATIONS
import org.jetbrains.kotlin.test.directives.LanguageSettingsDirectives.USE_INLINE_SCOPES_NUMBERS
import org.jetbrains.kotlin.test.directives.model.DirectivesContainer
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.FrontendKinds
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.utils.MultiModuleInfoDumper
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.util.Printer
import org.jetbrains.org.objectweb.asm.util.Textifier
import org.jetbrains.org.objectweb.asm.util.TraceFieldVisitor
import org.jetbrains.org.objectweb.asm.util.TraceMethodVisitor

class AsmLikeInstructionListingHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    companion object {
        const val DUMP_EXTENSION = "asm.txt"
        const val IR_DUMP_EXTENSION = "asm.ir.txt"
        const val INLINE_SCOPES_DUMP_EXTENSION = "asm.scopes.txt"
        const val FIR_DUMP_EXTENSION = "asm.fir.txt"
        const val LINE_SEPARATOR = "\n"

        val IGNORED_CLASS_VISIBLE_ANNOTATIONS = setOf(
            "Lkotlin/Metadata;",
            "Lkotlin/annotation/Target;",
            "Lkotlin/annotation/Retention;",
            "Ljava/lang/annotation/Retention;",
            "Ljava/lang/annotation/Target;"
        )
    }

    override val directiveContainers: List<DirectivesContainer>
        get() = listOf(AsmLikeInstructionListingDirectives)

    private val baseDumper = MultiModuleInfoDumper()

    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        if (CHECK_ASM_LIKE_INSTRUCTIONS !in module.directives) return
        val builder = baseDumper.builderForModule(module)
        val classes = info.classFileFactory
            .getClassFiles()
            .sortedBy { it.relativePath }
            .map { file -> ClassNode().also { ClassReader(file.asByteArray()).accept(it, ClassReader.EXPAND_FRAMES) } }

        val printBytecodeForTheseMethods = module.directives[CURIOUS_ABOUT]
        val showLocalVariables = LOCAL_VARIABLE_TABLE in module.directives
        val renderAnnotations = RENDER_ANNOTATIONS in module.directives

        classes.forEachIndexed { index, classNode ->
            builder.renderClassNode(classNode, printBytecodeForTheseMethods, showLocalVariables, renderAnnotations)
            builder.appendLine()
            if (index != classes.indices.last) {
                builder.appendLine()
            }
        }
    }

    private fun StringBuilder.renderClassNode(
        clazz: ClassNode,
        showBytecodeForTheseMethods: List<String>,
        showLocalVariables: Boolean,
        renderAnnotations: Boolean
    ) {
        val fields = (clazz.fields ?: emptyList()).sortedBy { it.name }
        val methods = (clazz.methods ?: emptyList()).sortedBy { it.name }

        val superTypes = (listOf(clazz.superName) + clazz.interfaces).filterNotNull()

        if (renderAnnotations) {
            clazz.signature?.let {
                appendLine(it)
            }
        }
        renderVisibilityModifiers(clazz.access)
        renderModalityModifiers(clazz.access)
        append(if ((clazz.access and Opcodes.ACC_INTERFACE) != 0) "interface " else "class ")
        append(clazz.name)

        if (superTypes.isNotEmpty()) {
            append(" : " + superTypes.joinToString())
        }

        appendLine(" {")

        if (renderAnnotations) {
            val textifier = Textifier()
            val visitor = TraceMethodVisitor(textifier)

            clazz.visibleAnnotations?.forEach {
                if (it.desc !in IGNORED_CLASS_VISIBLE_ANNOTATIONS) {
                    it.accept(visitor.visitAnnotation(it.desc, true))
                }
            }
            clazz.invisibleAnnotations?.forEach {
                it.accept(visitor.visitAnnotation(it.desc, false))
            }

            clazz.visibleTypeAnnotations?.forEach {
                it.accept(visitor.visitTypeAnnotation(it.typeRef, it.typePath, it.desc, true))
            }
            clazz.invisibleTypeAnnotations?.forEach {
                it.accept(visitor.visitTypeAnnotation(it.typeRef, it.typePath, it.desc, false))
            }

            textifier.getText().takeIf { it.isNotEmpty() }?.let {
                appendLine(textifier.getText().joinToString("").trimEnd())
                appendLine("")
            }
        }

        fields.joinTo(this, LINE_SEPARATOR.repeat(2)) { renderField(it, renderAnnotations).withMargin() }

        if (fields.isNotEmpty()) {
            appendLine().appendLine()
        }

        methods.joinTo(this, LINE_SEPARATOR.repeat(2)) {
            val showBytecode = showBytecodeForTheseMethods.contains(it.name)
            renderMethod(it, showBytecode, showLocalVariables, renderAnnotations).withMargin()
        }

        appendLine().append("}")
    }

    private fun renderField(field: FieldNode, renderAnnotations: Boolean): String = buildString {
        if (renderAnnotations) {
            field.signature?.let {
                append(it).append("\n")
            }
        }
        renderVisibilityModifiers(field.access)
        renderModalityModifiers(field.access)
        append(Type.getType(field.desc).className).append(' ')
        append(field.name)

        if (renderAnnotations) {
            val textifier = Textifier()
            val visitor = TraceFieldVisitor(textifier)

            field.visibleAnnotations?.forEach {
                it.accept(visitor.visitAnnotation(it.desc, true))
            }
            field.invisibleAnnotations?.forEach {
                it.accept(visitor.visitAnnotation(it.desc, false))
            }

            field.visibleTypeAnnotations?.forEach {
                it.accept(visitor.visitTypeAnnotation(it.typeRef, it.typePath, it.desc, true))
            }
            field.invisibleTypeAnnotations?.forEach {
                it.accept(visitor.visitTypeAnnotation(it.typeRef, it.typePath, it.desc, false))
            }
            textifier.getText().takeIf { it.isNotEmpty() }?.let {
                append("\n${textifier.getText().joinToString("").trimEnd()}")
            }
        }
    }

    private fun renderMethod(
        method: MethodNode,
        showBytecode: Boolean,
        showLocalVariables: Boolean,
        renderAnnotations: Boolean
    ): String = buildString {
        if (renderAnnotations) {
            method.signature?.let {
                append(it).append("\n")
            }
        }

        renderVisibilityModifiers(method.access)
        renderModalityModifiers(method.access)
        val (returnType, parameterTypes) = with(Type.getMethodType(method.desc)) { returnType to argumentTypes }
        append(returnType.className).append(' ')
        append(method.name)

        parameterTypes.mapIndexed { index, type ->
            val name = getParameterName(index, method)
            "${type.className} $name"
        }.joinTo(this, prefix = "(", postfix = ")")

        if (renderAnnotations) {
            val textifier = Textifier()
            val visitor = TraceMethodVisitor(textifier)

            method.visibleAnnotations?.forEach {
                it.accept(visitor.visitAnnotation(it.desc, true))
            }
            method.invisibleAnnotations?.forEach {
                it.accept(visitor.visitAnnotation(it.desc, false))
            }

            method.visibleTypeAnnotations?.forEach {
                it.accept(visitor.visitTypeAnnotation(it.typeRef, it.typePath, it.desc, true))
            }
            method.invisibleTypeAnnotations?.forEach {
                it.accept(visitor.visitTypeAnnotation(it.typeRef, it.typePath, it.desc, false))
            }

            method.visibleParameterAnnotations?.forEachIndexed { index, parameterAnnotations: MutableList<AnnotationNode>? ->
                parameterAnnotations?.forEach {
                    it.accept(
                        visitor.visitParameterAnnotation(
                            index,
                            it.desc,
                            true
                        )
                    )
                }
            }
            method.invisibleParameterAnnotations?.forEachIndexed { index, parameterAnnotations: MutableList<AnnotationNode>? ->
                parameterAnnotations?.forEach {
                    it.accept(
                        visitor.visitParameterAnnotation(
                            index,
                            it.desc,
                            false
                        )
                    )
                }
            }
            textifier.getText().takeIf { it.isNotEmpty() }?.let {
                append("\n${textifier.getText().joinToString("").trimEnd()}")
            }
        }

        val actualShowBytecode = showBytecode && (method.access and Opcodes.ACC_ABSTRACT) == 0
        val actualShowLocalVariables = showLocalVariables && method.localVariables?.takeIf { it.isNotEmpty() } != null

        if (actualShowBytecode || actualShowLocalVariables) {
            appendLine(" {")

            if (actualShowLocalVariables) {
                val localVariableTable = buildLocalVariableTable(method)
                if (localVariableTable.isNotEmpty()) {
                    append(localVariableTable.withMargin())
                }
            }

            if (actualShowBytecode) {
                if (actualShowLocalVariables) {
                    appendLine().appendLine()
                }
                append(renderBytecodeInstructions(method.instructions).trimEnd().withMargin())
            }

            appendLine().append("}")

            method.visibleTypeAnnotations
        }
    }

    private fun getParameterName(index: Int, method: MethodNode): String {
        val localVariableIndexOffset = when {
            (method.access and Opcodes.ACC_STATIC) != 0 -> 0
            method.isJvmOverloadsGenerated() -> 0
            else -> 1
        }

        val actualIndex = index + localVariableIndexOffset
        val localVariables = method.localVariables
        return localVariables?.firstOrNull {
            it.index == actualIndex
        }?.name ?: "p$index"
    }

    private fun buildLocalVariableTable(method: MethodNode): String {
        val localVariables = method.localVariables?.takeIf { it.isNotEmpty() } ?: return ""
        return buildString {
            append("Local variables:")
            for (variable in localVariables) {
                appendLine().append(("${variable.index} ${variable.name}: ${variable.desc}").withMargin())
            }
        }
    }

    private fun renderBytecodeInstructions(instructions: InsnList) = buildString {
        val labelMappings = LabelMappings()

        var currentInsn = instructions.first
        while (currentInsn != null) {
            renderInstruction(currentInsn, labelMappings)
            currentInsn = currentInsn.next
        }
    }

    private fun StringBuilder.renderInstruction(node: AbstractInsnNode, labelMappings: LabelMappings) {
        if (node is LabelNode) {
            appendLine("LABEL (L${labelMappings[node.label]})")
            return
        }

        if (node is LineNumberNode) {
            appendLine("LINENUMBER (${node.line})")
            return
        }

        if (node is FrameNode) return

        append("  ").append(Printer.OPCODES[node.opcode] ?: error("Invalid opcode ${node.opcode}"))

        when (node) {
            is FieldInsnNode -> append(" (${node.owner}, ${node.name}, ${node.desc})")
            is JumpInsnNode -> append(" (L${labelMappings[node.label.label]})")
            is IntInsnNode -> append(" (${node.operand})")
            is MethodInsnNode -> append(" (${node.owner}, ${node.name}, ${node.desc})")
            is VarInsnNode -> append(" (${node.`var`})")
            is LdcInsnNode -> append(" (${node.cst})")
            is TypeInsnNode -> append(" (${node.desc})")
            is IincInsnNode -> append(" (${node.`var`}, ${node.incr})")
            is MultiANewArrayInsnNode -> append(" (${node.desc}, ${node.dims})")
            is InvokeDynamicInsnNode -> append(" (${node.name}, ${node.desc}, ${node.bsm}, ${node.bsmArgs.joinToString()})")
        }

        appendLine()

        if (node is TableSwitchInsnNode || node is LookupSwitchInsnNode) {
            val (cases, default) = if (node is LookupSwitchInsnNode) {
                node.keys.zip(node.labels) to node.dflt
            } else {
                (node as TableSwitchInsnNode).min.rangeTo(node.max).zip(node.labels) to node.dflt
            }

            for ((key, labelNode) in cases) {
                appendLine("    $key: L${labelMappings[labelNode.label]}")
            }
            appendLine("    default: L${labelMappings[default.label]}")
        }
    }

    private fun String.withMargin(margin: String = "    "): String {
        return lineSequence().map { margin + it }.joinToString(LINE_SEPARATOR)
    }

    private fun StringBuilder.renderVisibilityModifiers(access: Int) {
        if ((access and Opcodes.ACC_PUBLIC) != 0) append("public ")
        if ((access and Opcodes.ACC_PRIVATE) != 0) append("private ")
        if ((access and Opcodes.ACC_PROTECTED) != 0) append("protected ")
    }

    private fun StringBuilder.renderModalityModifiers(access: Int) {
        if ((access and Opcodes.ACC_FINAL) != 0) append("final ")
        if ((access and Opcodes.ACC_ABSTRACT) != 0) append("abstract ")
        if ((access and Opcodes.ACC_STATIC) != 0) append("static ")
    }

    private class LabelMappings {
        private var mappings = hashMapOf<Int, Int>()
        private var currentIndex = 0

        operator fun get(label: Label): Int {
            val hashCode = System.identityHashCode(label)
            return mappings.getOrPut(hashCode) { currentIndex++ }
        }
    }

    private fun MethodNode.isJvmOverloadsGenerated(): Boolean {
        fun AnnotationNode.isJvmOverloadsGenerated() =
            this.desc == DefaultParameterValueSubstitutor.ANNOTATION_TYPE_DESCRIPTOR_FOR_JVM_OVERLOADS_GENERATED_METHODS

        return (visibleAnnotations?.any { it.isJvmOverloadsGenerated() } ?: false)
                || (invisibleAnnotations?.any { it.isJvmOverloadsGenerated() } ?: false)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {
        val firDifference = FIR_DIFFERENCE in testServices.moduleStructure.allDirectives
        val inlineScopesDifference = INLINE_SCOPES_DIFFERENCE in testServices.moduleStructure.allDirectives

        val firstModule = testServices.moduleStructure.modules.first()

        val inlineScopesNumbersEnabled = firstModule.directives.contains(USE_INLINE_SCOPES_NUMBERS)
        val extension = when {
            inlineScopesNumbersEnabled && inlineScopesDifference ->
                INLINE_SCOPES_DUMP_EXTENSION
            firDifference && firstModule.frontendKind == FrontendKinds.FIR ->
                FIR_DUMP_EXTENSION
            else ->
                DUMP_EXTENSION
        }

        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()
        val file = testDataFile.withExtension(extension)

        if (baseDumper.isEmpty()) {
            assertions.assertFileDoesntExist(file, CHECK_ASM_LIKE_INSTRUCTIONS)
            return
        }

        assertions.assertEqualsToFile(file, baseDumper.generateResultingDump())

        if (firDifference) {
            val irDump = testDataFile.withExtension(IR_DUMP_EXTENSION)
            val firDump = testDataFile.withExtension(FIR_DUMP_EXTENSION)
            if (irDump.exists() && firDump.exists()) {
                assertions.assertFalse(irDump.readText().trim() == firDump.readText().trim()) {
                    "Dumps for classic frontend and FIR are identical. Please remove $FIR_DIFFERENCE directive and ${firDump.name} file"
                }
            }
        }
    }
}
