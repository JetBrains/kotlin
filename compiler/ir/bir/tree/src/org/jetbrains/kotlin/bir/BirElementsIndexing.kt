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
    val condition: BirElementIndexMatcher?,
    val elementClass: Class<*>,
) : BirElementGeneralIndexerKey

fun interface BirElementIndexMatcher : BirElementGeneralIndexer {
    fun matches(element: BirElementBase): Boolean
}

internal fun interface BirElementIndexClassifier {
    fun classify(element: BirElementBase, minimumIndex: Int, backReferenceRecorder: BirForest.BackReferenceRecorder): Int
}


class BirElementBackReferencesKey<E : BirElement>(
    val recorder: BirElementBackReferenceRecorder,
    val elementClass: Class<*>,
) : BirElementGeneralIndexerKey

fun interface BirElementBackReferenceRecorder : BirElementGeneralIndexer {
    context(BirElementBackReferenceRecorderScope)
    fun recordBackReferences(element: BirElementBase)
}

interface BirElementBackReferenceRecorderScope {
    fun recordReference(forwardRef: BirElement?)
}


sealed interface BirElementGeneralIndexerKey

sealed interface BirElementGeneralIndexer {
    enum class Kind { IndexMatcher, BackReferenceRecorder }
}


internal object BirElementIndexClassifierFunctionGenerator {
    private val nextClassifierFunctionClassIdx = AtomicInteger(0)
    private val staticIndexerFunctionsInitializationBuffers = ConcurrentHashMap<String, Array<BirElementGeneralIndexer?>>()
    private val generatedFunctionClassLoader by lazy { ByteArrayFunctionClassLoader(BirElement::class.java.classLoader) }
    private val classifierFunctionClassCache = ConcurrentHashMap<Set<IndexerCacheKey>, Class<*>>()

    private data class IndexerCacheKey(val conditionClass: Class<*>?, val elementClass: Class<*>, val index: Int)

    class Indexer(
        val kind: BirElementGeneralIndexer.Kind,
        val indexerFunction: BirElementGeneralIndexer?,
        val elementClass: Class<*>,
        val index: Int,
    )

    fun createClassifierFunction(indexers: List<Indexer>): BirElementIndexClassifier {
        val matchersMaxIndex = indexers.maxOf { it.index }
        val indexersFunctions = arrayOfNulls<BirElementGeneralIndexer>(matchersMaxIndex + 1)
        for (matcher in indexers) {
            indexersFunctions[matcher.index] = matcher.indexerFunction
        }

        val clazz = getOrCreateClassifierFunctionClass(indexers, indexersFunctions)
        val instance = clazz.declaredConstructors.single().newInstance(indexersFunctions)
        return instance as BirElementIndexClassifier
    }

    private fun getOrCreateClassifierFunctionClass(
        indexers: List<Indexer>,
        indexersFunctions: Array<BirElementGeneralIndexer?>,
    ): Class<*> {
        val key = indexers.map { IndexerCacheKey(it.indexerFunction?.javaClass, it.elementClass, it.index) }.toHashSet()
        return classifierFunctionClassCache.computeIfAbsent(key) { _ ->
            val clazzNode = generateClassifierFunctionClass(indexers)
            val cw = ClassWriter(ClassWriter.COMPUTE_FRAMES)
            clazzNode.accept(cw)
            val binary = cw.toByteArray()
            val binaryName = clazzNode.name.replace('/', '.')

            staticIndexerFunctionsInitializationBuffers[clazzNode.name] = indexersFunctions
            generatedFunctionClassLoader.defineClass(binaryName, binary)
        }
    }

    private fun generateClassifierFunctionClass(indexers: List<Indexer>): ClassNode {
        val clazz = ClassNode().apply {
            version = Opcodes.V1_8
            access = Opcodes.ACC_PUBLIC
            val id = nextClassifierFunctionClassIdx.getAndIncrement()
            name = "org/jetbrains/kotlin/bir/BirElementIndexClassifierFunctionGenerator\$BirElementIndexClassifier$id"
            superName = "java/lang/Object"
            interfaces.add(Type.getInternalName(BirElementIndexClassifier::class.java))
        }

        val capturedMatcherInstancesCache = generateClassifyMethod(clazz, indexers)
        generateConstructor(clazz, capturedMatcherInstancesCache)
        generateStaticConstructor(clazz, capturedMatcherInstancesCache)

        return clazz
    }

    private fun generateClassifyMethod(
        clazz: ClassNode,
        indexers: List<Indexer>,
    ): Map<Indexer, FieldNode> {
        val classifyMethod = MethodNode().apply {
            access = Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL
            name = "classify"
            desc = Type.getMethodDescriptor(
                Type.INT_TYPE,
                Type.getType(BirElementBase::class.java),
                Type.INT_TYPE,
                Type.getType(BirForest.BackReferenceRecorder::class.java)
            )
        }

        val topLevelElementClassNodes = buildClassMatchingTree(indexers)

        val capturedIndexerInstances = mutableMapOf<Indexer, FieldNode>()
        for (indexer in indexers) {
            indexer.indexerFunction?.javaClass?.let { conditionFunctionClass ->
                val cacheInstanceInStaticField = conditionFunctionClass.declaredFields.isEmpty()
                val fieldIdx = capturedIndexerInstances.size
                val field = FieldNode(
                    Opcodes.ACC_PRIVATE + Opcodes.ACC_FINAL + if (cacheInstanceInStaticField) Opcodes.ACC_STATIC else 0,
                    "indexer$fieldIdx",
                    Type.getDescriptor(
                        when (indexer.kind) {
                            BirElementGeneralIndexer.Kind.IndexMatcher -> BirElementIndexMatcher::class.java
                            BirElementGeneralIndexer.Kind.BackReferenceRecorder -> BirElementBackReferenceRecorder::class.java
                        }
                    ),
                    null,
                    null,
                )
                clazz.fields.add(field)
                capturedIndexerInstances[indexer] = field
            }
        }

        val il = classifyMethod.instructions

        // result variable
        il.add(InsnNode(Opcodes.ICONST_0))
        il.add(VarInsnNode(Opcodes.ISTORE, 4))

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

            val allIndexers = descendantNodesAndSelf.flatMap { it.indexers }.sortedBy { it.index }
            for (indexer in allIndexers) {
                generateIndexerCase(il, indexer, capturedIndexerInstances[indexer], clazz)
            }

            il.add(JumpInsnNode(Opcodes.GOTO, isInstanceButNoMatchesLabel))
            il.add(notInstanceOfLabel)
        }

        val endLabel = LabelNode()
        for (node in topLevelElementClassNodes) {
            generateClassBranch(node, emptySequence(), endLabel)
        }

        il.add(endLabel)
        il.add(VarInsnNode(Opcodes.ILOAD, 4))
        il.add(InsnNode(Opcodes.IRETURN))

        clazz.methods.add(classifyMethod)

        return capturedIndexerInstances
    }

    private fun generateIndexerCase(il: InsnList, indexer: Indexer, indexerField: FieldNode?, clazz: ClassNode) {
        val matcherLabel = LabelNode()
        il.add(IntInsnNode(Opcodes.SIPUSH, indexer.index))
        il.add(VarInsnNode(Opcodes.ILOAD, 2))
        il.add(JumpInsnNode(Opcodes.IF_ICMPLT, matcherLabel))

        if (indexer.kind == BirElementGeneralIndexer.Kind.IndexMatcher) {
            // skip if already got result
            il.add(VarInsnNode(Opcodes.ILOAD, 4))
            il.add(JumpInsnNode(Opcodes.IFNE, matcherLabel))
        }

        if (indexerField != null) {
            if ((indexerField.access and Opcodes.ACC_STATIC) != 0) {
                il.add(FieldInsnNode(Opcodes.GETSTATIC, clazz.name, indexerField.name, indexerField.desc))
            } else {
                il.add(VarInsnNode(Opcodes.ALOAD, 0))
                il.add(FieldInsnNode(Opcodes.GETFIELD, clazz.name, indexerField.name, indexerField.desc))
            }
        }

        when (indexer.kind) {
            BirElementGeneralIndexer.Kind.IndexMatcher -> {
                if (indexerField != null) {
                    il.add(VarInsnNode(Opcodes.ALOAD, 1))
                    il.add(
                        MethodInsnNode(
                            Opcodes.INVOKEINTERFACE,
                            Type.getInternalName(BirElementIndexMatcher::class.java),
                            "matches",
                            Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(BirElementBase::class.java))
                        )
                    )
                    il.add(JumpInsnNode(Opcodes.IFEQ, matcherLabel))
                }

                il.add(IntInsnNode(Opcodes.SIPUSH, indexer.index))
                il.add(VarInsnNode(Opcodes.ISTORE, 4))
            }
            BirElementGeneralIndexer.Kind.BackReferenceRecorder -> {
                require(indexerField != null)
                il.add(VarInsnNode(Opcodes.ALOAD, 3))
                il.add(VarInsnNode(Opcodes.ALOAD, 1))
                il.add(
                    MethodInsnNode(
                        Opcodes.INVOKEINTERFACE,
                        Type.getInternalName(BirElementBackReferenceRecorder::class.java),
                        "recordBackReferences",
                        Type.getMethodDescriptor(
                            Type.VOID_TYPE,
                            Type.getType(BirElementBackReferenceRecorderScope::class.java),
                            Type.getType(BirElementBase::class.java)
                        )
                    )
                )
            }
        }

        il.add(matcherLabel)
    }

    private fun buildClassMatchingTree(indexers: List<Indexer>): MutableList<ElementClassNode> {
        val elementClassNodes = mutableMapOf<Class<*>, ElementClassNode>()
        for (indexer in indexers) {
            val node: ElementClassNode = elementClassNodes.computeIfAbsent(indexer.elementClass) { ElementClassNode(indexer.elementClass) }
            node.indexers += indexer
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
        val indexers = mutableListOf<Indexer>()
        val subNodes = mutableListOf<ElementClassNode>()
    }

    private fun generateConstructor(
        clazz: ClassNode,
        capturedIndexerInstances: Map<Indexer, FieldNode>,
    ) {
        val ctor = MethodNode().apply {
            access = Opcodes.ACC_PUBLIC
            name = "<init>"
            desc = "([${Type.getDescriptor(BirElementGeneralIndexer::class.java)})V"
        }

        val il = ctor.instructions
        il.add(VarInsnNode(Opcodes.ALOAD, 0))
        il.add(MethodInsnNode(Opcodes.INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false))
        capturedIndexerInstances.forEach { (matcher, field) ->
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
        capturedIndexerInstances: Map<Indexer, FieldNode>,
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
                "retrieveStaticIndexerFunctionsInitializationBuffer\$bir_tree",
                Type.getMethodDescriptor(Type.getType(Array<BirElementGeneralIndexer>::class.java), Type.getType(String::class.java))
            )
        )

        capturedIndexerInstances.forEach { (matcher, field) ->
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
    internal fun retrieveStaticIndexerFunctionsInitializationBuffer(className: String): Array<BirElementGeneralIndexer?> {
        return staticIndexerFunctionsInitializationBuffers.remove(className)!!
    }

    private class ByteArrayFunctionClassLoader(parent: ClassLoader) : ClassLoader(parent) {
        fun defineClass(name: String, binary: ByteArray): Class<*> {
            return defineClass(name, binary, 0, binary.size)
        }
    }
}
