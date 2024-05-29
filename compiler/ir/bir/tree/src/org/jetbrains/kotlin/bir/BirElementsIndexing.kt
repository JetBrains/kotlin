/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir

import org.jetbrains.kotlin.bir.util.ForwardReferenceRecorder
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.ClassWriter
import org.jetbrains.org.objectweb.asm.Opcodes
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.tree.*
import org.jetbrains.org.objectweb.asm.util.CheckClassAdapter
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KProperty1

class BirElementsIndexKey<E : BirElement>(
    val condition: BirElementIndexMatcher?,
    val elementType: BirElementType<E>,
) : BirElementGeneralIndexerKey

fun interface BirElementIndexMatcher : BirElementGeneralIndexer {
    fun matches(element: BirElementBase): Boolean
}

internal fun interface BirElementIndexClassifier {
    fun classify(element: BirElementBase, minimumIndex: Int, forwardReferenceRecorder: ForwardReferenceRecorder?): Int
}


class BirElementBackReferencesKey<E : BirElement, R : BirElement>(
    val recorder: BirElementBackReferenceRecorder<R>,
    val elementType: BirElementType<E>,
    val forwardReferenceProperty: KProperty1<E, *>?,
) : BirElementGeneralIndexerKey

fun interface BirElementBackReferenceRecorder<R : BirElement> : BirElementGeneralIndexer {
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

    private data class IndexerCacheKey(val conditionClass: Class<*>?, val elementType: BirElementType<*>, val index: Int)

    class Indexer(
        val kind: BirElementGeneralIndexer.Kind,
        val indexerFunction: BirElementGeneralIndexer?,
        val elementType: BirElementType<*>,
        val forwardReferenceProperty: KProperty1<*, *>?,
        val index: Int,
    )

    fun createClassifierFunction(indexers: List<Indexer>): BirElementIndexClassifier {
        val matchersMaxIndex = indexers.maxOfOrNull { it.index } ?: -1
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
        val key = indexers.map { IndexerCacheKey(it.indexerFunction?.javaClass, it.elementType, it.index) }.toHashSet()
        return classifierFunctionClassCache.computeIfAbsent(key) { _ ->
            val clazzNode = generateClassifierFunctionClass(indexers)
            val classWriter = ClassWriter(ClassWriter.COMPUTE_FRAMES)

            var classVisitor: ClassVisitor = classWriter
            classVisitor = CheckClassAdapter(classVisitor, false)
            clazzNode.accept(classVisitor)

            val binary = classWriter.toByteArray()
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

        val capturedIndexerInstances = storeIndexerInstances(indexers, clazz)
        val capturedMatcherInstancesCache = generateClassifyMethod(clazz, indexers, capturedIndexerInstances)
        generateConstructor(clazz, capturedMatcherInstancesCache)
        generateStaticConstructor(clazz, capturedMatcherInstancesCache)

        return clazz
    }


    private fun storeIndexerInstances(indexers: List<Indexer>, clazz: ClassNode): Map<Indexer, FieldNode> {
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
        return capturedIndexerInstances
    }

    private fun generateClassifyMethod(
        clazz: ClassNode,
        indexers: List<Indexer>,
        capturedIndexerInstances: Map<Indexer, FieldNode>,
    ): Map<Indexer, FieldNode> {
        val classifyMethod = MethodNode().apply {
            access = Opcodes.ACC_PUBLIC + Opcodes.ACC_FINAL
            name = "classify"
            desc = Type.getMethodDescriptor(
                Type.INT_TYPE,
                Type.getType(BirElementBase::class.java),
                Type.INT_TYPE,
                Type.getType(ForwardReferenceRecorder::class.java)
            )
        }

        val il = classifyMethod.instructions
        val elementVarIdx = 1
        val minIndexVarIdx = 2
        val referenceRecorderVarIdx = 3
        val resultVarIdx = 4

        il.add(InsnNode(Opcodes.ICONST_0))
        il.add(VarInsnNode(Opcodes.ISTORE, resultVarIdx))

        if (indexers.isNotEmpty()) {
            val indexersIndexMin = indexers.minOf { it.index }
            val indexersIndicesSpan = indexers.maxOf { it.index } - indexersIndexMin + 1

            // Compute a key to the switch table.
            // key = element.classId * indexersIndicesSpan + (minIndex - indexersIndexMin)
            il.add(VarInsnNode(Opcodes.ALOAD, elementVarIdx))
            il.add(
                MethodInsnNode(
                    Opcodes.INVOKEVIRTUAL,
                    Type.getInternalName(BirElementBase::class.java),
                    BirElementBase::class.java.declaredMethods.single { method ->
                        method.name.startsWith("getElementClassId-") && method.name.count { it == '$' } < 2
                    }.name,
                    Type.getMethodDescriptor(Type.BYTE_TYPE)
                )
            )
            il.add(IntInsnNode(Opcodes.SIPUSH, indexersIndicesSpan))
            il.add(InsnNode(Opcodes.IMUL))
            il.add(VarInsnNode(Opcodes.ILOAD, minIndexVarIdx))
            il.add(IntInsnNode(Opcodes.SIPUSH, indexersIndexMin))
            il.add(InsnNode(Opcodes.ISUB))
            il.add(InsnNode(Opcodes.IADD))

            val switchInstPlaceholder = InsnNode(Opcodes.NOP)
            il.add(switchInstPlaceholder)

            val elementSwitchNodes = buildClassMatchingStructure(indexers)
            val switchCases = mutableListOf<Pair<LabelNode, Int>>()
            val endLabel = LabelNode()
            for (node in elementSwitchNodes) {
                val indexersByIndex = node.indexers.associateBy { it.index }
                var lastFallthroughLabel: LabelNode? = null
                for (index in indexersIndexMin..node.indexers.maxOf { it.index }) {
                    val indexer = indexersByIndex[index]

                    val caseLabel = if (indexer == null) {
                        lastFallthroughLabel ?: LabelNode().also {
                            il.add(it)
                            lastFallthroughLabel = it
                        }
                    } else {
                        lastFallthroughLabel = null
                        LabelNode().also { il.add(it) }
                    }

                    for (elementClass in node.elementClasses) {
                        val switchKey = elementClass.id * indexersIndicesSpan + (index - indexersIndexMin)
                        switchCases += caseLabel to switchKey
                    }

                    if (indexer != null) {
                        generateIndexerCase(
                            il, indexer, capturedIndexerInstances[indexer], clazz,
                            elementVarIdx, referenceRecorderVarIdx, resultVarIdx
                        )
                    }
                }

                il.add(JumpInsnNode(Opcodes.GOTO, endLabel))
            }

            switchCases.sortBy { it.second }
            il.set(
                switchInstPlaceholder,
                LookupSwitchInsnNode(endLabel, switchCases.map { it.second }.toIntArray(), switchCases.map { it.first }.toTypedArray())
            )

            il.add(endLabel)
        }

        il.add(VarInsnNode(Opcodes.ILOAD, resultVarIdx))
        il.add(InsnNode(Opcodes.IRETURN))

        clazz.methods.add(classifyMethod)

        return capturedIndexerInstances
    }

    private fun generateIndexerCase(
        il: InsnList, indexer: Indexer, indexerField: FieldNode?, clazz: ClassNode,
        elementVarIdx: Int, referenceRecorderVarIdx: Int, resultVarIdx: Int,
    ) {
        val matcherLabel = LabelNode()

        when (indexer.kind) {
            BirElementGeneralIndexer.Kind.IndexMatcher -> {
                // skip if already got a result
                il.add(VarInsnNode(Opcodes.ILOAD, resultVarIdx))
                il.add(JumpInsnNode(Opcodes.IFNE, matcherLabel))
            }
            BirElementGeneralIndexer.Kind.BackReferenceRecorder -> {
                il.add(VarInsnNode(Opcodes.ALOAD, referenceRecorderVarIdx))
                il.add(JumpInsnNode(Opcodes.IFNULL, matcherLabel))
            }
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
                    il.add(VarInsnNode(Opcodes.ALOAD, elementVarIdx))
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
                il.add(VarInsnNode(Opcodes.ISTORE, resultVarIdx))
            }
            BirElementGeneralIndexer.Kind.BackReferenceRecorder -> {
                require(indexerField != null)
                il.add(VarInsnNode(Opcodes.ALOAD, referenceRecorderVarIdx))
                il.add(VarInsnNode(Opcodes.ALOAD, elementVarIdx))
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

    private fun buildClassMatchingStructure(indexers: List<Indexer>): List<ElementSwitchNode> {
        val elementClassNodes = BirMetadata.allElements.associateWith { ElementClassInfo(it) }

        for (indexer in indexers) {
            for (topLevelClass in indexer.elementType.possibleClasses) {
                topLevelClass.descendantClassesAndSelf.forEach { elementClass ->
                    val node = elementClassNodes.getValue(elementClass)
                    if (node.elementClass.hasImplementation) {
                        node.indexers += indexer
                    }
                }
            }
        }

        for (element in elementClassNodes.values) {
            // todo: check if properties are in the same type hierarchy
            //  (so that one overrides the other), or they just happen
            //  to have the same name.
            val mergedBackRefRecorders = element.indexers
                .filter { it.kind == BirElementGeneralIndexer.Kind.BackReferenceRecorder }
                .filter { it.forwardReferenceProperty != null }
                .groupBy { it.forwardReferenceProperty!!.name }
                .filter { it.value.size > 1 }
            element.indexers.removeAll { indexer ->
                mergedBackRefRecorders.any { group ->
                    // retain only the last one
                    indexer in group.value && group.value.any { it.index > indexer.index }
                }
            }
        }

        for (element in elementClassNodes.values) {
            element.indexers.sortBy { it.index }
        }

        return elementClassNodes.values
            .filter { it.indexers.isNotEmpty() }
            .sortedBy { it.elementClass.id }
            .groupBy { it.indexers }
            .map { group ->
                ElementSwitchNode(group.value.map { it.elementClass }, group.key)
            }
    }

    private class ElementClassInfo(
        val elementClass: BirElementClass<*>,
    ) {
        val indexers = mutableListOf<Indexer>()
    }

    private class ElementSwitchNode(
        val elementClasses: List<BirElementClass<*>>,
        val indexers: List<Indexer>,
    )

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
