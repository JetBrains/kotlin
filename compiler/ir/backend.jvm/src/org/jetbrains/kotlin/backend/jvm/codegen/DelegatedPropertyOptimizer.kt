/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.jvm.codegen

import org.jetbrains.kotlin.codegen.optimization.common.asSequence
import org.jetbrains.kotlin.codegen.optimization.common.isMeaningful
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.utils.addToStdlib.safeAs
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.tree.*

class DelegatedPropertyOptimizer {
    private var usedDelegatedProperties = mutableSetOf<Int>()
    private var useOfDelegatesPropertiesArray = false

    val needsDelegatedProperties: Boolean
        get() = useOfDelegatesPropertiesArray || usedDelegatedProperties.isNotEmpty()

    // Remove unused loads of cached KProperties from the given method and record uses of cached KProperties.
    fun transform(methodNode: MethodNode) {
        class KPropertyRanges(val propertyIndex: Int, val ranges: MutableList<Pair<AbstractInsnNode, AbstractInsnNode>> = mutableListOf())

        val variableToDelegatedPropertyIndex = mutableMapOf<Int, KPropertyRanges>()

        // We look for bytecode patterns of the form
        //
        //   getstatic $$delegatedProperties:[Lkotlin/reflect/KProperty;
        //   iconst <n>
        //   aaload
        //   (checkcast ...)
        //   astore <x>
        //
        // If the variable `x` is used, we mark the delegated property at index `n` as used,
        // otherwise we delete the code in question. This is the pattern that results from an
        // inline call with a cached KProperty argument. If the `astore` instruction is missing
        // then that's a different use of the KProperty at index `n` and we mark it as used.
        // Finally, if there is any other code which loads the `$$delegatedProperties` array,
        // we mark it as generally unsafe to remove.
        for (insn in methodNode.instructions) {
            if (!insn.isDelegatedPropertiesArray)
                continue
            var index = 0
            var current = matchRange(insn) {
                index = iconst()
                aaload()
            }

            if (current == null) {
                // There's an unknown use of the $$delegatedProperties array, we need to ensure that we don't remove it.
                useOfDelegatesPropertiesArray = true
                return
            }

            var slot = 0
            current = matchRange(current) {
                slot = astore()
            }

            if (current == null) {
                // There's an actual use of the delegated property at `index`, mark it.
                usedDelegatedProperties.add(index)
            } else {
                variableToDelegatedPropertyIndex.getOrPut(slot) {
                    KPropertyRanges(index)
                }.ranges += insn to current
            }
        }

        if (variableToDelegatedPropertyIndex.isEmpty())
            return

        // Mark all of the KProperty variables which are used in the current method.
        methodNode.instructions.asSequence().filterIsInstance<VarInsnNode>().filter { it.opcode == Opcodes.ALOAD }.forEach {
            variableToDelegatedPropertyIndex[it.`var`]?.let { kPropertyRange ->
                usedDelegatedProperties.add(kPropertyRange.propertyIndex)
                variableToDelegatedPropertyIndex.remove(it.`var`)
            }
        }

        // Remove the unused KProperty loads.
        val locals = methodNode.localVariables.mapTo(mutableSetOf()) { it.index }
        for (kPropertyRanges in variableToDelegatedPropertyIndex.values) {
            for ((from, to) in kPropertyRanges.ranges) {
                // Stores to variables with LVT entries need to be preserved. We store `null` for
                // unused KProperties, for compatibility with the JVM BE.
                val storeInsn = to as VarInsnNode
                if (storeInsn.`var` !in locals) {
                    methodNode.instructions.removeRange(from, to)
                } else {
                    methodNode.instructions.removeRange(from, to.previous)
                    methodNode.instructions.insertBefore(to, InsnNode(Opcodes.ACONST_NULL))
                }
            }
        }
    }

    // Remove the initialization of unused cached KProperties, or remove the $$delegatedProperties field initialization
    // completely, if it is not used.
    fun transformClassInitializer(methodNode: MethodNode) {
        if (useOfDelegatesPropertiesArray)
            return

        for (insn in methodNode.instructions) {
            // Parse the array allocation
            //
            //    LDC <length>
            //    ANEWARRAY kotlin/reflect/KProperty
            //    ASTORE <x>
            //
            val size = insn.intValue ?: continue
            var slot = 0
            var current = matchRange(insn) {
                anewarray("kotlin/reflect/KProperty")
                slot = astore()
            } ?: continue

            // Note that there can be cases where some property references are used, in which case we
            // can only remove the initialization code for some of the references, not the whole
            // $$delegatedProperties array.
            val rangesToRemove = if (usedDelegatedProperties.isNotEmpty()) {
                mutableListOf<Pair<AbstractInsnNode, AbstractInsnNode>>()
            } else {
                null
            }

            // Parse the array element initializers
            for (i in 0 until size) {
                var index = 0
                val start = current.next
                // The code to initialize a single array element looks like this
                //
                //   ALOAD <x>
                //
                //   LDC <index>
                //
                //   NEW kotlin/jvm/internal/(Mutable)PropertyReference[012]Impl
                //   DUP
                //   LDC <current>.class
                //   LDC "<module>"
                //   INVOKESTATIC kotlin/jvm/internal/Reflection.getOrCreateKotlinPackage (Ljava/lang/Class;Ljava/lang/String;)Lkotlin/reflect/KDeclarationContainer;
                //   LDC "<property name>"
                //   LDC "<property getter signature>"
                //   INVOKESPECIAL kotlin/jvm/internal/(Mutable)PropertyReference[012]Impl.<init> (Lkotlin/reflect/KDeclarationContainer;Ljava/lang/String;Ljava/lang/String;)V
                //   INVOKESTATIC kotlin/jvm/internal/Reflection.(mutable)property[012] (Lkotlin/jvm/internal/PropertyReference[012];)Lkotlin/reflect/KProperty[012];
                //
                //   AASTORE
                //
                // With the caveat that we might create a class instead of a package as the `KDeclarationContainer`.
                current = matchRange(current) {
                    aload(slot)
                    index = iconst()
                    new(PROPERTY_REFERENCE_CLASSES)
                    dup()
                    // Either getOrCreateKotlinPackage(class, module) or getOrCreateKotlinClass(class)
                    ldc() // java class
                    optional {
                        ldc() // module name
                    }
                    invokestatic("kotlin/jvm/internal/Reflection")
                    ldc() // name
                    ldc() // signature
                    invokespecial("<init>", "(Lkotlin/reflect/KDeclarationContainer;Ljava/lang/String;Ljava/lang/String;)V")
                    invokestatic("kotlin/jvm/internal/Reflection")
                    aastore()
                } ?: break

                if (rangesToRemove != null && index !in usedDelegatedProperties) {
                    rangesToRemove.add(start to current)
                }
            }

            // At this point the code sets the $$delegatedProperties field to the constructed array.
            //
            //   ALOAD <x>
            //   PUTSTATIC <current>.$$delegatedProperties : [Lkotlin/reflect/KProperty;
            current = matchRange(current) {
                aload(slot)
                putstatic(JvmAbi.DELEGATED_PROPERTIES_ARRAY_NAME, "[Lkotlin/reflect/KProperty;")
            } ?: continue

            if (rangesToRemove == null) {
                // Remove the whole array initialization
                methodNode.instructions.removeRange(insn, current)
            } else {
                for ((from, to) in rangesToRemove) {
                    methodNode.instructions.removeRange(from, to)
                }
            }

            // There is only one initializer for the $$delegatedProperties array.
            return
        }
    }

    // Match a half-open range of instructions from start (exclusive!) and returns either null if the instructions
    // did not match or the last instruction which matched.
    private fun matchRange(start: AbstractInsnNode, body: InstructionMatcher.() -> Unit): AbstractInsnNode? =
        InstructionMatcher(start).apply(body).current

    companion object {
        val PROPERTY_REFERENCE_CLASSES = setOf(
            "kotlin/jvm/internal/PropertyReference0Impl",
            "kotlin/jvm/internal/PropertyReference1Impl",
            "kotlin/jvm/internal/PropertyReference2Impl",
            "kotlin/jvm/internal/MutablePropertyReference0Impl",
            "kotlin/jvm/internal/MutablePropertyReference1Impl",
            "kotlin/jvm/internal/MutablePropertyReference2Impl",
        )
    }
}

// Match a straight-line segment of instructions, while ignoring irrelevant instructions such as labels, linenumbers,
// frames, nops, and casts.
private class InstructionMatcher(var current: AbstractInsnNode?) {
    fun peek(): AbstractInsnNode? {
        var insn = current?.next ?: return null
        while (!insn.isMeaningful || insn.opcode == Opcodes.NOP || insn.opcode == Opcodes.CHECKCAST) {
            insn = insn.next ?: return null
        }
        return insn
    }

    inline fun match(predicate: (AbstractInsnNode) -> Boolean) {
        current = peek()?.takeIf(predicate)
    }

    inline fun <T> parse(default: T, op: (AbstractInsnNode) -> T?): T {
        var value = default
        match {
            op(it)?.let {
                value = it
                true
            } ?: false
        }
        return value
    }

    inline fun optional(block: () -> Unit) {
        val start = current
        block()
        if (current == null) {
            current = start
        }
    }

    fun iconst(): Int = parse(0) { it.intValue }

    fun aaload() {
        match { it.opcode == Opcodes.AALOAD }
    }

    fun aastore() {
        match { it.opcode == Opcodes.AASTORE }
    }

    fun anewarray(desc: String) {
        match { it.opcode == Opcodes.ANEWARRAY && it is TypeInsnNode && it.desc == desc }
    }

    fun aload(slot: Int) {
        match { it.opcode == Opcodes.ALOAD && it is VarInsnNode && it.`var` == slot }
    }

    fun astore(): Int =
        parse(0) { insn -> insn.safeAs<VarInsnNode>()?.takeIf { it.opcode == Opcodes.ASTORE }?.`var` }

    fun putstatic(name: String, desc: String) {
        match { it.opcode == Opcodes.PUTSTATIC && it is FieldInsnNode && it.name == name && it.desc == desc }
    }

    fun dup() {
        match { it.opcode == Opcodes.DUP }
    }

    fun ldc() {
        match { it.opcode == Opcodes.LDC }
    }

    fun invokestatic(owner: String) {
        match { it.opcode == Opcodes.INVOKESTATIC && it is MethodInsnNode && it.owner == owner }
    }

    fun invokespecial(name: String, desc: String) {
        match { it.opcode == Opcodes.INVOKESPECIAL && it is MethodInsnNode && it.name == name && it.desc == desc }
    }

    fun new(classNames: Set<String>) {
        match { it.opcode == Opcodes.NEW && it is TypeInsnNode && it.desc in classNames }
    }
}

private fun InsnList.safeRemove(insn: AbstractInsnNode) {
    if (insn !is LabelNode && insn !is LineNumberNode)
        remove(insn)
}

private fun InsnList.removeRange(from: AbstractInsnNode, to: AbstractInsnNode) {
    var current = from
    do {
        val next = current.next
        safeRemove(current)
        current = next
    } while (current != to)
    safeRemove(to)
}

private val AbstractInsnNode.isDelegatedPropertiesArray: Boolean
    get() = opcode == Opcodes.GETSTATIC && this is FieldInsnNode
            && name == JvmAbi.DELEGATED_PROPERTIES_ARRAY_NAME && desc == "[Lkotlin/reflect/KProperty;"

private val AbstractInsnNode.intValue: Int?
    get() = when (opcode) {
        Opcodes.ICONST_0 -> 0
        Opcodes.ICONST_1 -> 1
        Opcodes.ICONST_2 -> 2
        Opcodes.ICONST_3 -> 3
        Opcodes.ICONST_4 -> 4
        Opcodes.ICONST_5 -> 5
        Opcodes.ICONST_M1 -> -1
        Opcodes.BIPUSH, Opcodes.SIPUSH -> safeAs<IntInsnNode>()?.operand
        Opcodes.LDC -> safeAs<LdcInsnNode>()?.cst as? Int
        else -> null
    }
