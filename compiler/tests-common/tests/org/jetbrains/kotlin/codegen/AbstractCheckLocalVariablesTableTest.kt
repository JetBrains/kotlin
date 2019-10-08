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

package org.jetbrains.kotlin.codegen

import com.intellij.openapi.util.text.StringUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.backend.common.output.OutputFileCollection
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.org.objectweb.asm.*
import java.io.File
import java.util.*
import java.util.regex.Pattern

/**
 * Test correctness of written local variables in class file for specified method
 */
abstract class AbstractCheckLocalVariablesTableTest : CodegenTestCase() {
    override fun doMultiFileTest(wholeFile: File, files: List<TestFile>) {
        compile(files)

        try {
            val classAndMethod = parseClassAndMethodSignature(wholeFile)
            val split = classAndMethod.split("\\.".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
            assert(split.size == 2) { "Exactly one dot is expected: $classAndMethod" }
            val classFileRegex = StringUtil.escapeToRegexp(split[0] + ".class").replace("\\*", ".+")
            val methodName = split[1]

            val outputFiles = (classFileFactory as OutputFileCollection).asList()
            val outputFile = outputFiles.first { file -> file.relativePath.matches(classFileRegex.toRegex()) }

            val pathsString = outputFiles.joinToString { it.relativePath }
            assertNotNull("Couldn't find class file for pattern $classFileRegex in: $pathsString", outputFile)

            val classReader = ClassReader(outputFile.asByteArray())
            val actualLocalVariables = readLocalVariable(classReader, methodName)
            checkLocalVariableTypes(classReader, methodName, actualLocalVariables)

            doCompare(wholeFile, files.single().content, actualLocalVariables)
        } catch (e: Throwable) {
            printReport(wholeFile)
            throw e
        }
    }

    protected open fun doCompare(
        testFile: File,
        text: String,
        actualLocalVariables: List<LocalVariable>
    ) {
        KotlinTestUtils.assertEqualsToFile(
            testFile,
            text.substringBefore("// VARIABLE : ") + getActualVariablesAsString(
                actualLocalVariables
            )
        )
    }

    protected class LocalVariable internal constructor(
        val name: String,
        val type: String,
        val index: Int,
        val startLabelNumber: Int,
        val endLabelNumber: Int
    ) {

        override fun toString(): String {
            return "// VARIABLE : NAME=$name TYPE=$type INDEX=$index"
        }
    }

    private fun parseClassAndMethodSignature(testFile: File): String {
        for (line in testFile.readLines()) {
            val methodMatcher = methodPattern.matcher(line)
            if (methodMatcher.matches()) {
                return methodMatcher.group(1)
            }
        }

        throw AssertionError("method instructions not found")
    }

    companion object {

        private fun getActualVariablesAsString(list: List<LocalVariable>) = list.joinToString("\n")

        private val methodPattern = Pattern.compile("^// METHOD : *(.*)")

        private fun readLocalVariable(cr: ClassReader, methodName: String): List<LocalVariable> {

            class Visitor : ClassVisitor(Opcodes.API_VERSION) {
                var readVariables: MutableList<LocalVariable> = ArrayList()
                var methodFound = false

                override fun visitMethod(
                    access: Int, name: String, desc: String, signature: String?, exceptions: Array<String>?
                ): MethodVisitor? {
                    return if (methodName == name + desc) {
                        methodFound = true
                        object : MethodVisitor(Opcodes.API_VERSION) {
                            // ASM labels cannot be easily compared across two visits of the same method.
                            // Therefore, we keep our own numbering that is consistent across multiple
                            // visits.
                            private var currentLabelNumber = 0
                            private val labelToNumber: MutableMap<Label, Int> = mutableMapOf()

                            override fun visitLocalVariable(
                                name: String, desc: String, signature: String?, start: Label, end: Label, index: Int
                            ) {
                                readVariables.add(LocalVariable(name, desc, index, labelToNumber[start]!!, labelToNumber[end]!!))
                            }

                            override fun visitLabel(label: Label?) {
                                labelToNumber[label!!] = currentLabelNumber++
                            }
                        }
                    } else {
                        super.visitMethod(access, name, desc, signature, exceptions)
                    }
                }
            }

            val visitor = Visitor()
            cr.accept(visitor, ClassReader.SKIP_FRAMES)
            TestCase.assertTrue("method not found: $methodName", visitor.methodFound)
            return visitor.readVariables
        }

        private fun checkLocalVariableTypes(cr: ClassReader, methodName: String, locals: List<LocalVariable>) {

            // Representation of local load and store instruction.
            open class Instruction(val index: Int, val type: String)
            class Load(index: Int, type: String): Instruction(index, type)
            class Store(index: Int, type: String): Instruction(index, type)

            // Representation of basic blocks with the locals table at this block
            // as well as the actual locals at entry to the block computed
            // based on the recorded load and store instructions in the code.
            class BasicBlock(
                val successors: MutableSet<BasicBlock> = mutableSetOf(),
                var endsWithUnconditionalJump: Boolean = false,
                var localsTable: MutableMap<Int, String> = mutableMapOf(),
                var localsAtEntry: MutableMap<Int, String> = mutableMapOf(),
                val localsInstructions: MutableList<Instruction> = mutableListOf()
            ) {
                fun addInstruction(index: Int, opcode: Int) {
                    localsInstructions.add(
                        when (opcode) {
                            Opcodes.ILOAD -> Load(index, "I")
                            Opcodes.LLOAD -> Load(index, "J")
                            Opcodes.FLOAD -> Load(index, "F")
                            Opcodes.DLOAD -> Load(index, "D")
                            Opcodes.ALOAD -> Load(index, "Ljava/lang/Object;")
                            Opcodes.ISTORE -> Store(index, "I")
                            Opcodes.LSTORE -> Store(index, "J")
                            Opcodes.FSTORE -> Store(index, "F")
                            Opcodes.DSTORE -> Store(index, "D")
                            Opcodes.ASTORE -> Store(index, "Ljava/lang/Object;")
                            else -> throw Exception("Unsupported var instruction: $opcode")
                        }
                    )
                }
            }

            // Visitor that builds a control-flow graph of a method. The control-flow graph
            // consists of [BasicBlock]s containing the local load and store instructions
            // and the locals table at each basic block. When the visitor ends, the type of
            // local slots are computed based on instructions in the code and are checked
            // for consistency with information in the locals table.
            class Visitor : ClassVisitor(Opcodes.API_VERSION) {
                var methodFound = false
                var skipValidation = false

                var entryBlock = BasicBlock()
                var allBlocks: MutableSet<BasicBlock> = mutableSetOf(entryBlock)
                var currentBlock = entryBlock
                var labelToBlock: MutableMap<Label, BasicBlock> = mutableMapOf()
                val currentLocalsTable: MutableMap<Int, String> = mutableMapOf()

                private fun ensureBlock(label: Label): BasicBlock {
                    return if (labelToBlock.containsKey(label)) {
                        labelToBlock[label]!!
                    } else {
                        val result = BasicBlock()
                        allBlocks.add(result)
                        labelToBlock[label] = result
                        result
                    }
                }

                private fun recordLocalTypesForParameters(access: Int, desc: String) {
                    val localsAtEntry = entryBlock.localsAtEntry
                    var parameterIndex = 0
                    if (access and Opcodes.ACC_STATIC == 0) {
                        localsAtEntry[parameterIndex++] = "Ljava/lang/Object;"
                    }
                    Type.getMethodType(desc).argumentTypes.forEach {
                        localsAtEntry[parameterIndex] = when (it) {
                            // Bytecode instructions use iload/istore for all of these so we do not distinguish.
                            Type.BOOLEAN_TYPE,
                            Type.CHAR_TYPE,
                            Type.BYTE_TYPE,
                            Type.SHORT_TYPE,
                            Type.INT_TYPE -> "I"
                            Type.FLOAT_TYPE-> "F"
                            Type.DOUBLE_TYPE -> "D"
                            Type.LONG_TYPE -> "J";
                            else -> "Ljava/lang/Object;"
                        }
                        parameterIndex += it.size;
                    }
                }

                override fun visitMethod(
                    access: Int, name: String, desc: String, signature: String?, exceptions: Array<String>?
                ): MethodVisitor? {
                    return if (methodName == name + desc) {
                        methodFound = true

                        recordLocalTypesForParameters(access, desc)

                        object : MethodVisitor(Opcodes.API_VERSION) {
                            // ASM labels cannot be easily compared across to visits of the same method.
                            // Therefore, we keep our own numbering that is consistent across multiple
                            // visits.
                            private var currentLabelNumber = 0

                            // Record local load and store instructions for each basic block.
                            override fun visitVarInsn(opcode: Int, index: Int) {
                                currentBlock.addInstruction(index, opcode)
                            }

                            // Build control-flow graph based on jump instructions.
                            override fun visitJumpInsn(opcode: Int, label: Label?) {
                                when (opcode) {
                                    Opcodes.IFEQ,
                                    Opcodes.IFNE,
                                    Opcodes.IFLT,
                                    Opcodes.IFGE,
                                    Opcodes.IFGT,
                                    Opcodes.IFLE,
                                    Opcodes.IF_ICMPEQ,
                                    Opcodes.IF_ICMPNE,
                                    Opcodes.IF_ICMPLT,
                                    Opcodes.IF_ICMPGE,
                                    Opcodes.IF_ICMPGT,
                                    Opcodes.IF_ICMPLE,
                                    Opcodes.IF_ACMPEQ,
                                    Opcodes.IF_ACMPNE,
                                    Opcodes.IFNULL,
                                    Opcodes.IFNONNULL -> {
                                        val target = ensureBlock(label!!)
                                        val fallthrough = BasicBlock()
                                        fallthrough.localsTable = currentLocalsTable.toMutableMap()
                                        allBlocks.add(fallthrough)
                                        currentBlock.successors.add(target)
                                        currentBlock.successors.add(fallthrough)
                                        currentBlock = fallthrough
                                    }

                                    Opcodes.GOTO -> {
                                        currentBlock.endsWithUnconditionalJump = true
                                        currentBlock.successors.add(ensureBlock(label!!))
                                    }

                                    else -> throw Exception("Unsupported jump instruction $opcode")
                                }
                            }

                            // Skip validation for control-flow we do not yet support.
                            // TODO: Implement these to extend coverage.
                            override fun visitTryCatchBlock(start: Label?, end: Label?, handler: Label?, type: String?) {
                                skipValidation = true
                            }

                            override fun visitLookupSwitchInsn(dflt: Label?, keys: IntArray?, labels: Array<out Label>?) {
                                skipValidation = true
                            }

                            override fun visitTableSwitchInsn(min: Int, max: Int, dflt: Label?, vararg labels: Label?) {
                                skipValidation = true
                            }

                            // Start a new basic block at all labels and compute the active locals table.
                            override fun visitLabel(label: Label?) {
                                val newBlock = ensureBlock(label!!)
                                if (!currentBlock.endsWithUnconditionalJump) {
                                    currentBlock.successors.add(newBlock)
                                }
                                currentBlock = newBlock
                                // Compute locals table for new block. End expiring locals first and then
                                // introduce the ones that start at this label.
                                locals.forEach {
                                    if (it.endLabelNumber == currentLabelNumber) {
                                        currentLocalsTable.remove(it.index)
                                    }
                                }
                                locals.forEach {
                                    if (it.startLabelNumber == currentLabelNumber) {
                                        if (currentLocalsTable.containsKey(it.index)) {
                                            throw Exception("Locals table already contains info for slot ${it.index} at label: $label")
                                        }
                                        // We treat all integer types interchangably as the java bytecode uses
                                        // iload and istore for all of them so we cannot distinguish in our
                                        // analysis of the code.
                                        currentLocalsTable[it.index] = when (it.type) {
                                            "S", "Z", "C", "B" -> "I"
                                            else -> it.type
                                        }
                                    }
                                }
                                currentBlock.localsTable = currentLocalsTable.toMutableMap()
                                currentLabelNumber++
                            }
                        }
                    } else {
                        super.visitMethod(access, name, desc, signature, exceptions)
                    }
                }

                override fun visitEnd() {
                    if (skipValidation) return
                    propagateAndCheckTypes()
                }

                // Compute the types of locals based on the code and check that the locals table
                // is consistent with the types computed for the code.
                //
                // Currently this only checks that basic types are consistent and that basic
                // types and object types are not mixed. It does not check that two object types
                // are actually related. We consider all object types (including arrays) as equal
                // for this analysis.
                private fun propagateAndCheckTypes() {
                    // We need to go through all blocks at least once to make sure
                    // we propagate everything. After that, it is a fixed-point
                    // algorithm using a worklist: when the entry state of a block
                    // changes we enqueue that block for reprocessing.
                    val worklist: MutableList<BasicBlock> = LinkedList(allBlocks)
                    while (worklist.size > 0) {
                        val currentBlock = worklist.removeAt(0)
                        val currentLocals = currentBlock.localsAtEntry.toMutableMap()
                        // Check consistency with the local table.
                        for ((index, type) in currentBlock.localsTable) {
                            currentLocals[index]?.let {
                                checkCompatible(index, type, it)
                            } ?: throw Exception("Uninitialized local in locals table: index: $index type: $type")
                        }
                        // Check that loads make sense and change the actual locals types based
                        // on store instructions to compute the locals types at exit from this
                        // block.
                        for (inst in currentBlock.localsInstructions) {
                            when (inst) {
                                is Load -> {
                                    currentLocals[inst.index]?.let {
                                        checkCompatible(inst.index, it, inst.type)
                                    } ?: throw Exception("Uninitialized local read: index: ${inst.index} type: ${inst.type}")
                                }

                                is Store -> currentLocals[inst.index] = inst.type
                            }
                        }
                        // Propagate the type information to successor blocks and enqueue successor
                        // blocks for reprocessing if the type of the locals at entry changed.
                        for (succ in currentBlock.successors) {
                            if (!succ.localsAtEntry.equals(currentLocals)) {
                                for ((index, type) in currentLocals) {
                                    succ.localsAtEntry[index]?.let {
                                        // If conflicting types flow to the same block for a local
                                        // slot that is OK as long as the type is never used. Such
                                        // conflicting types are represented with a CONFLICT type
                                        // which will never match a read instruction or a type in
                                        // a locals table.
                                        if (!areCompatible(index, it, type)) {
                                            currentLocals[index] = "CONFLICT"
                                        }
                                    }
                                }
                                succ.localsAtEntry = currentLocals.toMutableMap()
                                worklist.add(succ)
                            }
                        }
                    }

                }

                // We only check that there is no confusion between object types and basic types.
                // Therefore, we map all arrays types to type Object when comparing.
                private fun checkCompatible(index: Int, type0: String, type1: String) {
                    if (areCompatible(index, type0, type1)) return
                    throw Exception("Incompatible types for local $index: $type0 and $type1")
                }

                private fun areCompatible(index: Int, type0: String, type1: String): Boolean {
                    val t0 = if (type0.startsWith("[")) "Ljava/lang/Object;" else type0
                    val t1 = if (type1.startsWith("[")) "Ljava/lang/Object;" else type1
                    if (t0.equals(t1)) return true
                    // If both are object descriptors we are fine, otherwise we have a mix
                    // of an object type and a basic type.
                    return t0.endsWith(";") == t1.endsWith(";")
                }
            }

            val visitor = Visitor()
            cr.accept(visitor, ClassReader.SKIP_FRAMES)
            TestCase.assertTrue("method not found: $methodName", visitor.methodFound)
        }
    }
}

