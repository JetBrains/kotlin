/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen.ir

import org.jetbrains.kotlin.codegen.AbstractLineNumberTest
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.config.JVMConfigurationKeys
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.testFramework.KtUsefulTestCase
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.MethodVisitor
import org.jetbrains.org.objectweb.asm.Opcodes
import java.io.File

abstract class AbstractIrLineNumberTest : AbstractLineNumberTest() {
    override fun extractConfigurationKind(files: MutableList<TestFile>): ConfigurationKind {
        return ConfigurationKind.ALL
    }

    override fun updateConfiguration(configuration: CompilerConfiguration) {
        super.updateConfiguration(configuration)
        configuration.put(JVMConfigurationKeys.IR, true)
    }

    override fun compareCustom(psiFile: KtFile, wholeFile: File) {
        val fileText = psiFile.text
        val expectedLineNumbers = normalize(
            fileText.substring(fileText.indexOf("//") + 2)
                .trim().split(" ").map { it.trim() }.toMutableList()
        )
        val actualLineNumbers = normalize(extractActualLineNumbersFromBytecode(classFileFactory, false))
        KtUsefulTestCase.assertSameElements(actualLineNumbers, expectedLineNumbers)
    }

    override fun readAllLineNumbers(reader: ClassReader) =
        normalize(super.readAllLineNumbers(reader))

    override fun extractSelectedLineNumbersFromSource(file: KtFile) =
        normalize(super.extractSelectedLineNumbersFromSource(file))

    override fun getTestFunLineNumbersMethodVisitor(
        labels: ArrayList<Label>,
        labels2LineNumbers: java.util.HashMap<Label, String>
    ): MethodVisitor {
        return object : MethodVisitor(Opcodes.ASM5) {
            private var lastLabel: Label? = null
            private var lastLine = -1

            override fun visitMethodInsn(opcode: Int, owner: String, name: String, desc: String, itf: Boolean) {
                if (LINE_NUMBER_FUN == name) {
                    labels.add(lastLabel ?: error("A function call with no preceding label"))
                }
            }

            override fun visitLabel(label: Label) {
                if (lastLabel != null && !labels2LineNumbers.containsKey(lastLabel) && lastLine >= 0) {
                    labels2LineNumbers[lastLabel!!] = Integer.toString(lastLine) // Inherited line number
                }
                lastLabel = label
            }

            override fun visitLineNumber(line: Int, start: Label) {
                labels2LineNumbers[start] = Integer.toString(line)
                lastLine = line
            }
        }
    }

    override fun readTestFunLineNumbers(cr: ClassReader) =
        normalize(super.readTestFunLineNumbers(cr))

    private fun normalize(numbers: List<String>) =
        numbers
            .map { if (it.startsWith('+')) it.substring(1) else it }
            .toSet()
            .toMutableList()
            .sortedBy { it.toInt() }
            .toList()
}
