/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Opcodes.*
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.util.Printer
import java.io.File

private val LINE_SEPARATOR = System.getProperty("line.separator")

abstract class AbstractAsmLikeInstructionListingTest : CodegenTestCase() {
    private companion object {
        val CURIOUS_ABOUT_DIRECTIVE = "// CURIOUS_ABOUT "
        val LOCAL_VARIABLE_TABLE_DIRECTIVE = "// LOCAL_VARIABLE_TABLE"
    }

    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>, javaFilesDir: File?) {
        val txtFile = File(wholeFile.parentFile, wholeFile.nameWithoutExtension + ".txt")
        compile(files, javaFilesDir)

        val classes = classFileFactory
                .getClassFiles()
                .sortedBy { it.relativePath }
                .map { file -> ClassNode().also { ClassReader(file.asByteArray()).accept(it, ClassReader.EXPAND_FRAMES) } }

        val testFileLines = wholeFile.readLines()

        val printBytecodeForTheseMethods = testFileLines
                .filter { it.startsWith(CURIOUS_ABOUT_DIRECTIVE) }
                .map { it.substring(CURIOUS_ABOUT_DIRECTIVE.length) }
                .flatMap { it.split(',').map { it.trim() } }

        val showLocalVariables = testFileLines.any { it.trim() == LOCAL_VARIABLE_TABLE_DIRECTIVE }

        KotlinTestUtils.assertEqualsToFile(txtFile, classes.joinToString(LINE_SEPARATOR.repeat(2)) {
            renderClassNode(it, printBytecodeForTheseMethods, showLocalVariables)
        })
    }

    private fun renderClassNode(clazz: ClassNode, showBytecodeForTheseMethods: List<String>, showLocalVariables: Boolean): String {
        val fields = (clazz.fields ?: emptyList()).sortedBy { it.name }
        val methods = (clazz.methods ?: emptyList()).sortedBy { it.name }

        val superTypes = (listOf(clazz.superName) + clazz.interfaces).filterNotNull()

        return buildString {
            renderVisibilityModifiers(clazz.access)
            renderModalityModifiers(clazz.access)
            append(if ((clazz.access and ACC_INTERFACE) != 0) "interface " else "class ")
            append(clazz.name)

            if (superTypes.isNotEmpty()) {
                append(" : " + superTypes.joinToString())
            }

            appendln(" {")

            fields.joinTo(this, LINE_SEPARATOR.repeat(2)) { renderField(it).withMargin() }

            if (fields.isNotEmpty()) {
                appendln().appendln()
            }

            methods.joinTo(this, LINE_SEPARATOR.repeat(2)) {
                val showBytecode = showBytecodeForTheseMethods.contains(it.name)
                renderMethod(it, showBytecode, showLocalVariables).withMargin()
            }

            appendln().append("}")
        }
    }

    private fun renderField(field: FieldNode) = buildString {
        renderVisibilityModifiers(field.access)
        renderModalityModifiers(field.access)
        append(Type.getType(field.desc).className).append(' ')
        append(field.name)
    }

    private fun renderMethod(method: MethodNode, showBytecode: Boolean, showLocalVariables: Boolean) = buildString {
        renderVisibilityModifiers(method.access)
        renderModalityModifiers(method.access)
        val (returnType, parameterTypes) = with(Type.getMethodType(method.desc)) { returnType to argumentTypes }
        append(returnType.className).append(' ')
        append(method.name)

        parameterTypes.mapIndexed { index, type ->
            val name = getParameterName(index, method)
            "${type.className} $name"
        }.joinTo(this, prefix = "(", postfix = ")")

        val actualShowBytecode = showBytecode && (method.access and ACC_ABSTRACT) == 0
        val actualShowLocalVariables = showLocalVariables && method.localVariables?.takeIf { it.isNotEmpty() } != null

        if (actualShowBytecode || actualShowLocalVariables) {
            appendln(" {")

            if (actualShowLocalVariables) {
                val localVariableTable = buildLocalVariableTable(method)
                if (localVariableTable.isNotEmpty()) {
                    append(localVariableTable.withMargin())
                }
            }

            if (actualShowBytecode) {
                if (actualShowLocalVariables) {
                    appendln().appendln()
                }
                append(renderBytecodeInstructions(method.instructions).trimEnd().withMargin())
            }

            appendln().append("}")
        }
    }

    private fun getParameterName(index: Int, method: MethodNode): String {
        val localVariableIndexOffset = when {
            (method.access and Opcodes.ACC_STATIC) != 0 -> 0
            method.isJvmOverloadsGenerated() -> 0
            else -> 1
        }

        val actualIndex = index + localVariableIndexOffset
        val localVariables = method.localVariables?.takeIf { it.size > actualIndex } ?: return "p$index"
        return localVariables[actualIndex].name
    }

    private fun buildLocalVariableTable(method: MethodNode): String {
        val localVariables = method.localVariables?.takeIf { it.isNotEmpty() } ?: return ""
        return buildString {
            append("Local variables:")
            for (variable in localVariables) {
                appendln().append((variable.index.toString() + " " + variable.name + ": " + variable.desc).withMargin())
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
            appendln("LABEL (L" + labelMappings[node.label] + ")")
            return
        }

        if (node is LineNumberNode) {
            appendln("LINENUMBER (" + node.line + ")")
            return
        }

        if (node is FrameNode) return

        append("  ").append(Printer.OPCODES[node.opcode] ?: error("Invalid opcode ${node.opcode}"))

        when (node) {
            is FieldInsnNode -> append(" (" + node.name + ", " + node.desc + ")")
            is JumpInsnNode -> append(" (L" + labelMappings[node.label.label] + ")")
            is IntInsnNode -> append(" (" + node.operand + ")")
            is MethodInsnNode -> append(" (" + node.owner + ", "+ node.name + ", " + node.desc + ")")
            is VarInsnNode -> append(" (" + node.`var` + ")")
            is LdcInsnNode -> append(" (" + node.cst + ")")
        }

        appendln()
    }

    private fun String.withMargin(margin: String = "    "): String {
        return lineSequence().map { margin + it }.joinToString(LINE_SEPARATOR)
    }

    private fun StringBuilder.renderVisibilityModifiers(access: Int) {
        if ((access and ACC_PUBLIC) != 0) append("public ")
        if ((access and ACC_PRIVATE) != 0) append("private ")
        if ((access and ACC_PROTECTED) != 0) append("protected ")
    }

    private fun StringBuilder.renderModalityModifiers(access: Int) {
        if ((access and ACC_FINAL) != 0) append("final ")
        if ((access and ACC_ABSTRACT) != 0) append("abstract ")
        if ((access and ACC_STATIC) != 0) append("static ")
    }

    private class LabelMappings {
        private var mappings = hashMapOf<Int, Int>()
        private var currentIndex = 0

        operator fun get(label: Label): Int {
            val hashCode = System.identityHashCode(label)
            return mappings.getOrPut(hashCode) { currentIndex++ }
        }
    }
}

private fun MethodNode.isJvmOverloadsGenerated(): Boolean {
    fun AnnotationNode.isJvmOverloadsGenerated() =
        this.desc == DefaultParameterValueSubstitutor.ANNOTATION_TYPE_DESCRIPTOR_FOR_JVM_OVERLOADS_GENERATED_METHODS

    return (visibleAnnotations?.any { it.isJvmOverloadsGenerated() } ?: false)
            || (invisibleAnnotations?.any { it.isJvmOverloadsGenerated() } ?: false)
}