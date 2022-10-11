/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental

import org.jetbrains.kotlin.incremental.storage.ProtoMapValue
import org.jetbrains.kotlin.inline.inlineFunctionsAndAccessors
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.metadata.jvm.deserialization.BitEncoding
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMemberSignature
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.*

/**
 * Minimal information about a Kotlin class to compute recompilation-triggering changes during an incremental run of the `KotlinCompile`
 * task (see [IncrementalJvmCache.saveClassToCache]).
 *
 * It's important that this class contain only the minimal required information, as it will be part of the classpath snapshot of the
 * `KotlinCompile` task and the task needs to support compile avoidance. For example, this class should contain public method signatures,
 * and should not contain private method signatures, or method implementations.
 */
class KotlinClassInfo constructor(
    val classId: ClassId,
    val classKind: KotlinClassHeader.Kind,
    val classHeaderData: Array<String>, // Can be empty
    val classHeaderStrings: Array<String>, // Can be empty
    val multifileClassName: String?, // Not null iff classKind == KotlinClassHeader.Kind.MULTIFILE_CLASS_PART
    val constantsMap: LinkedHashMap<String, Any>,
    val inlineFunctionsAndAccessorsMap: LinkedHashMap<String, Long>
) {

    val className: JvmClassName by lazy { JvmClassName.byClassId(classId) }

    val protoMapValue: ProtoMapValue by lazy {
        ProtoMapValue(
            isPackageFacade = classKind != KotlinClassHeader.Kind.CLASS,
            BitEncoding.decodeBytes(classHeaderData),
            classHeaderStrings
        )
    }

    /**
     * The [ProtoData] of this class.
     *
     * NOTE: The caller needs to ensure `classKind != KotlinClassHeader.Kind.MULTIFILE_CLASS` first, as the compiler doesn't write proto
     * data to [KotlinClassHeader.Kind.MULTIFILE_CLASS] classes.
     */
    val protoData: ProtoData by lazy {
        check(classKind != KotlinClassHeader.Kind.MULTIFILE_CLASS) {
            "Proto data is not available for KotlinClassHeader.Kind.MULTIFILE_CLASS: $classId"
        }
        protoMapValue.toProtoData(classId.packageFqName)
    }

    /** Name of the companion object of this class (default is "Companion") iff this class HAS a companion object, or null otherwise. */
    val companionObject: ClassId? by lazy {
        if (classKind == KotlinClassHeader.Kind.CLASS) {
            (protoData as ClassProtoData).getCompanionObjectName()?.let {
                classId.createNestedClassId(Name.identifier(it))
            }
        } else null
    }

    /** List of constants defined in this class iff this class IS a companion object, or null otherwise. The list could be empty. */
    val constantsInCompanionObject: List<String>? by lazy {
        if (classKind == KotlinClassHeader.Kind.CLASS) {
            val classProtoData = protoData as ClassProtoData
            if (classProtoData.proto.isCompanionObject) {
                classProtoData.getConstants()
            } else null
        } else null
    }

    companion object {

        fun createFrom(kotlinClass: LocalFileKotlinClass): KotlinClassInfo {
            return createFrom(kotlinClass.classId, kotlinClass.classHeader, kotlinClass.fileContents)
        }

        fun createFrom(classId: ClassId, classHeader: KotlinClassHeader, classContents: ByteArray): KotlinClassInfo {
            val (constants, inlineFunctionsAndAccessors) = getConstantsAndInlineFunctionsOrAccessors(classHeader, classContents)

            return KotlinClassInfo(
                classId,
                classHeader.kind,
                classHeader.data ?: emptyArray(),
                classHeader.strings ?: emptyArray(),
                classHeader.multifileClassName,
                constants.mapKeysTo(LinkedHashMap()) { it.key.name },
                inlineFunctionsAndAccessors.mapKeysTo(LinkedHashMap()) { it.key.asString() },
            )
        }
    }
}

/**
 * Parses the class file only once to get both constants and inline functions/property accessors. This is faster than getting them
 * separately in two passes.
 */
private fun getConstantsAndInlineFunctionsOrAccessors(
    classHeader: KotlinClassHeader,
    classContents: ByteArray
): Pair<Map<JvmMemberSignature.Field, Any>, Map<JvmMemberSignature.Method, Long>> {
    val constantsClassVisitor = ConstantsClassVisitor()
    val inlineFunctionsAndAccessors = inlineFunctionsAndAccessors(classHeader)

    return if (inlineFunctionsAndAccessors.isEmpty()) {
        // parsingOptions = (SKIP_CODE, SKIP_DEBUG) as method bodies and debug info are not important for constants
        ClassReader(classContents).accept(constantsClassVisitor, ClassReader.SKIP_CODE or ClassReader.SKIP_DEBUG)
        Pair(constantsClassVisitor.getResult(), emptyMap())
    } else {
        val inlineFunctionsAndAccessorsClassVisitor =
            InlineFunctionsAndAccessorsClassVisitor(inlineFunctionsAndAccessors.toSet(), constantsClassVisitor)
        // parsingOptions must not include (SKIP_CODE, SKIP_DEBUG) as method bodies and debug info (e.g., line numbers) are important for
        // inline functions/accessors
        ClassReader(classContents).accept(inlineFunctionsAndAccessorsClassVisitor, 0)
        Pair(constantsClassVisitor.getResult(), inlineFunctionsAndAccessorsClassVisitor.getResult())
    }
}

private class ConstantsClassVisitor : ClassVisitor(Opcodes.API_VERSION) {
    private val result = mutableMapOf<JvmMemberSignature.Field, Any>()

    override fun visitField(access: Int, name: String, desc: String, signature: String?, value: Any?): FieldVisitor? {
        if (access and Opcodes.ACC_PRIVATE == Opcodes.ACC_PRIVATE) return null

        val staticFinal = Opcodes.ACC_STATIC or Opcodes.ACC_FINAL
        if (value != null && access and staticFinal == staticFinal) {
            result[JvmMemberSignature.Field(name, desc)] = value
        }
        return null
    }

    fun getResult() = result
}

private class InlineFunctionsAndAccessorsClassVisitor(
    private val inlineFunctionsAndAccessors: Set<JvmMemberSignature.Method>,
    cv: ConstantsClassVisitor // Note: cv must not override `visitMethod` (it will not be called with the current implementation below)
) : ClassVisitor(Opcodes.API_VERSION, cv) {

    private val result = mutableMapOf<JvmMemberSignature.Method, Long>()
    private var classVersion: Int? = null

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<out String>?) {
        super.visit(version, access, name, signature, superName, interfaces)
        classVersion = version
    }

    override fun visitMethod(access: Int, name: String, desc: String, signature: String?, exceptions: Array<out String>?): MethodVisitor? {
        if (access and Opcodes.ACC_PRIVATE == Opcodes.ACC_PRIVATE) return null

        val method = JvmMemberSignature.Method(name, desc)
        if (method !in inlineFunctionsAndAccessors) return null

        val classWriter = ClassWriter(0)

        // The `version` and `name` parameters are important (see KT-38857), the others can be null.
        classWriter.visit(/* version */ classVersion!!, /* access */ 0, /* name */ "ClassWithOneMethod", null, null, null)

        return object : MethodVisitor(Opcodes.API_VERSION, classWriter.visitMethod(access, name, desc, signature, exceptions)) {
            override fun visitEnd() {
                result[method] = classWriter.toByteArray().md5()
            }
        }
    }

    fun getResult() = result
}
