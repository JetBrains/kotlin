/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class BirElementsIndexKey<E : BirElement>(
    val condition: BirElementIndexMatcher, // todo: use it
    val elementClass: Class<*>,
    val includeOtherModules: Boolean,
) {
    internal var index = -1
}

fun interface BirElementIndexMatcher {
    fun matches(element: BirElementBase): Boolean
}

fun interface BirElementIndexClassifier {
    fun classify(element: BirElementBase, minimumIndex: Int): Int
}

object BirElementIndexClassifierFunctionGenerator {
    private val nextClassifierFunctionClassIdx = AtomicInteger(0)
    private val classifierFunctionClassCache = ConcurrentHashMap<Set<MatcherCacheKey>, Class<*>>()
    private val staticConditionLambdasInitializationBuffers = ConcurrentHashMap<String, Array<BirElementIndexMatcher?>>()
    private val generatedFunctionClassLoader by lazy { ByteArrayFunctionClassLoader(BirElement::class.java.classLoader) }

    private val BirElementIndexClassifierMatchFunctionName by lazy {
        // in case of using some kotlin features, like inline classes, compiler might obfuscate the name,
        //   so it does not exactly match the source name
        BirElementIndexMatcher::class.java.declaredMethods.single { it.name.startsWith("matches") }.name
    }

    class Matcher(val condition: BirElementIndexMatcher, val elementClass: Class<*>, val index: Int)

    fun createClassifierFunction(matchers: List<Matcher>): BirElementIndexClassifier {
        val matchersMaxIndex = matchers.maxOf { it.index }
        val conditionsArray = arrayOfNulls<BirElementIndexMatcher>(matchersMaxIndex + 1)
        for (matcher in matchers) {
            conditionsArray[matcher.index] = matcher.condition
        }

        val clazz = getOrCreateClassifierFunctionClass(matchers, conditionsArray)
        val instance = clazz.declaredConstructors.single().newInstance(conditionsArray)
        return instance as BirElementIndexClassifier
    }

    private fun getOrCreateClassifierFunctionClass(
        matchers: List<Matcher>,
        conditionsArray: Array<BirElementIndexMatcher?>,
    ): Class<*> {
        val key = matchers.map { MatcherCacheKey(it.condition.javaClass, it.elementClass, it.index) }.toHashSet()
        return classifierFunctionClassCache.computeIfAbsent(key) { _ ->
            val clazzNode = generateClassifierFunctionClass(matchers)
            val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
            clazzNode.accept(cw)
            val binary = cw.toByteArray()
            val binaryName = clazzNode.name.replace('/', '.')

            staticConditionLambdasInitializationBuffers[clazzNode.name] = conditionsArray
            generatedFunctionClassLoader.defineClass(binaryName, binary)
        }
    }

    private fun generateClassifierFunctionClass(matchers: List<Matcher>): ClassNode {
        val clazz = ClassNode().apply {
            version = Opcodes.V1_8
            access = Opcodes.ACC_PUBLIC
            val id = nextClassifierFunctionClassIdx.getAndIncrement()
            name = "org/jetbrains/kotlin/bir/BirElementIndexClassifierFunctionGenerator\$BirElementIndexClassifier$id"
            superName = "java/lang/Object"
            interfaces.add(Type.getInternalName(BirElementIndexClassifier::class.java))
        }

        val capturedMatcherInstancesCache = generateClassifyMethod(clazz, matchers)
        generateConstructor(clazz, capturedMatcherInstancesCache)
        generateStaticConstructor(clazz, capturedMatcherInstancesCache)

        return clazz
    }

    private fun generateClassifyMethod(
        clazz: ClassNode,
        matchers: List<Matcher>,
    ): Map<Matcher, FieldNode> {
        val classifyMethod = MethodNode().apply {
            access = Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL
            name = "classify"
            desc = Type.getMethodDescriptor(Type.INT_TYPE, Type.getType(BirElementBase::class.java), Type.INT_TYPE)
        }

        val topLevelElementClassNodes = buildClassMatchingTree(matchers)

        val capturedMatcherInstances = mutableMapOf<Matcher, FieldNode>()
        for (matcher in matchers) {
            val conditionFunctionClass = matcher.condition.javaClass

            val cacheInstanceInStaticField = conditionFunctionClass.declaredFields.isEmpty()
            val fieldIdx = capturedMatcherInstances.size
            val field = FieldNode(
                Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL + if (cacheInstanceInStaticField) Opcodes.ACC_STATIC else 0,
                "matcher$fieldIdx",
                Type.getDescriptor(BirElementIndexMatcher::class.java),
                null,
                null,
            )
            clazz.fields.add(field)
            capturedMatcherInstances[matcher] = field
        }

        val il = classifyMethod.instructions
        fun generateClassBranch(
            node: ElementClassNode,
            descendantNodes: Sequence<ElementClassNode>,
            isInstanceButNoMatchesLabel: LabelNode,
        ) {
            il.add(VarInsnNode(Opcodes.ALOAD, 1))
            il.add(TypeInsnNode(Opcodes.INSTANCEOF, Type.getInternalName(node.elementClass)))
            val notInstanceOfLabel = LabelNode()
            il.add(JumpInsnNode(Opcodes.IFEQ, notInstanceOfLabel))

            val descendantNodesAndSelf = descendantNodes + node
            for (subNode in node.subNodes) {
                generateClassBranch(subNode, descendantNodesAndSelf, notInstanceOfLabel)
            }

            val allMatchers = descendantNodesAndSelf.flatMap { it.matchers }.sortedBy { it.index }
            for (matcher in allMatchers) {
                generateMatchCase(il, matcher, capturedMatcherInstances.getValue(matcher), clazz)
            }

            il.add(JumpInsnNode(Opcodes.GOTO, isInstanceButNoMatchesLabel))
            il.add(notInstanceOfLabel)
        }

        val endLabel = LabelNode()
        for (node in topLevelElementClassNodes) {
            generateClassBranch(node, emptySequence(), endLabel)
        }

        il.add(endLabel)
        il.add(InsnNode(Opcodes.ICONST_0))
        il.add(InsnNode(Opcodes.IRETURN))

        clazz.methods.add(classifyMethod)

        return capturedMatcherInstances
    }

    private fun generateMatchCase(il: InsnList, matcher: Matcher, matcherField: FieldNode, clazz: ClassNode) {
        val matcherLabel = LabelNode()
        il.add(IntInsnNode(Opcodes.SIPUSH, matcher.index))
        il.add(VarInsnNode(Opcodes.ILOAD, 2))
        il.add(JumpInsnNode(Opcodes.IF_ICMPLT, matcherLabel))

        if ((matcherField.access and Opcodes.ACC_STATIC) != 0) {
            il.add(FieldInsnNode(Opcodes.GETSTATIC, clazz.name, matcherField.name, matcherField.desc))
        } else {
            il.add(VarInsnNode(Opcodes.ALOAD, 0))
            il.add(FieldInsnNode(Opcodes.GETFIELD, clazz.name, matcherField.name, matcherField.desc))
        }

        il.add(VarInsnNode(Opcodes.ALOAD, 1))
        il.add(
            MethodInsnNode(
                Opcodes.INVOKEINTERFACE,
                Type.getInternalName(BirElementIndexMatcher::class.java),
                BirElementIndexClassifierMatchFunctionName,
                Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(BirElementBase::class.java))
            )
        )
        il.add(JumpInsnNode(Opcodes.IFEQ, matcherLabel))

        il.add(IntInsnNode(Opcodes.SIPUSH, matcher.index))
        il.add(InsnNode(Opcodes.IRETURN))
        il.add(matcherLabel)
    }

    private fun buildClassMatchingTree(matchers: List<Matcher>): MutableList<ElementClassNode> {
        val elementClassNodes = mutableMapOf<Class<*>, ElementClassNode>()
        for (matcher in matchers) {
            val node: ElementClassNode = elementClassNodes.computeIfAbsent(matcher.elementClass) { ElementClassNode(matcher.elementClass) }
            node.matchers += matcher
        }

        val topLevelElementClassNodes = elementClassNodes.values.toMutableList()
        topLevelElementClassNodes.removeAll { node ->
            val visitedTypes = hashSetOf<Class<*>>()
            var isTopLevel = true
            fun visitType(type: Class<*>?, isDescendant: Boolean) {
                if (type == null || type == Any::class.java) {
                    return
                }

                if (type == BirElement::class.java || type == BirElementBase::class.java) {
                    return
                }

                if (!visitedTypes.add(type)) {
                    return
                }

                if (isDescendant) {
                    val parentNode = elementClassNodes[type]
                    if (parentNode != null) {
                        parentNode.subNodes += node
                        isTopLevel = false
                        return
                    }
                }

                visitType(type.superclass, true)
                for (parentType in type.interfaces) {
                    visitType(parentType, true)
                }
            }
            visitType(node.elementClass, false)

            !isTopLevel
        }
        return topLevelElementClassNodes
    }

    private class ElementClassNode(val elementClass: Class<*>) {
        val matchers = mutableListOf<Matcher>()
        val subNodes = mutableListOf<ElementClassNode>()
    }

    private fun generateConstructor(
        clazz: ClassNode,
        capturedMatcherInstances: Map<Matcher, FieldNode>,
    ) {
        val ctor = MethodNode().apply {
            access = Opcodes.ACC_PUBLIC
            name = "<init>"
            desc = "([${Type.getDescriptor(BirElementIndexMatcher::class.java)})V"
        }

        val il = ctor.instructions
        il.add(VarInsnNode(Opcodes.ALOAD, 0))
        il.add(MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false))
        capturedMatcherInstances.forEach { (matcher, field) ->
            val isStatic = (field.access and Opcodes.ACC_STATIC) != 0
            if (!isStatic) {
                il.add(VarInsnNode(Opcodes.ALOAD, 0))
                il.add(VarInsnNode(Opcodes.ALOAD, 1))
                il.add(IntInsnNode(Opcodes.SIPUSH, matcher.index))
                il.add(InsnNode(Opcodes.AALOAD))
                il.add(TypeInsnNode(Opcodes.CHECKCAST, Type.getType(field.desc).internalName))
                il.add(FieldInsnNode(Opcodes.PUTFIELD, clazz.name, field.name, field.desc))
            }
        }
        il.add(InsnNode(Opcodes.RETURN))

        clazz.methods.add(ctor)
    }

    private fun generateStaticConstructor(
        clazz: ClassNode,
        capturedMatcherInstances: Map<Matcher, FieldNode>,
    ) {
        val ctor = MethodNode().apply {
            access = Opcodes.ACC_PUBLIC + Opcodes.ACC_STATIC
            name = "<clinit>"
            desc = "()V"
        }

        val il = ctor.instructions
        il.add(LdcInsnNode(clazz.name))
        il.add(
            MethodInsnNode(
                Opcodes.INVOKESTATIC,
                Type.getInternalName(BirElementIndexClassifierFunctionGenerator::class.java),
                "retrieveStaticConditionLambdasInitializationBuffer\$bir_tree",
                Type.getMethodDescriptor(Type.getType(Array<BirElementIndexMatcher>::class.java), Type.getType(String::class.java))
            )
        )

        capturedMatcherInstances.forEach { (matcher, field) ->
            val isStatic = (field.access and Opcodes.ACC_STATIC) != 0
            if (isStatic) {
                il.add(InsnNode(Opcodes.DUP))
                il.add(IntInsnNode(Opcodes.SIPUSH, matcher.index))
                il.add(InsnNode(Opcodes.AALOAD))
                il.add(TypeInsnNode(Opcodes.CHECKCAST, Type.getType(field.desc).internalName))
                il.add(FieldInsnNode(Opcodes.PUTSTATIC, clazz.name, field.name, field.desc))
            }
        }

        il.add(InsnNode(Opcodes.POP))
        il.add(InsnNode(Opcodes.RETURN))

        clazz.methods.add(ctor)
    }

    @JvmStatic
    @Suppress("unused") // used by generated code
    internal fun retrieveStaticConditionLambdasInitializationBuffer(className: String): Array<BirElementIndexMatcher?> {
        return staticConditionLambdasInitializationBuffers.remove(className)!!
    }

    private data class MatcherCacheKey(val conditionClass: Class<*>, val elementClass: Class<*>, val index: Int)

    private class ByteArrayFunctionClassLoader(parent: ClassLoader) : ClassLoader(parent) {
        fun defineClass(name: String, binary: ByteArray): Class<*> {
            return defineClass(name, binary, 0, binary.size)
        }
    }
}
