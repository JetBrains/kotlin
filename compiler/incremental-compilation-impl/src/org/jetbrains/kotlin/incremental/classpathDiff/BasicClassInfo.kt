/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.incremental.classpathDiff

import org.jetbrains.kotlin.load.kotlin.FileBasedKotlinClass.*
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.header.ReadKotlinClassHeaderAnnotationVisitor
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.org.objectweb.asm.AnnotationVisitor
import org.jetbrains.org.objectweb.asm.ClassReader
import org.jetbrains.org.objectweb.asm.ClassReader.SKIP_CODE
import org.jetbrains.org.objectweb.asm.ClassReader.SKIP_DEBUG
import org.jetbrains.org.objectweb.asm.ClassVisitor
import org.jetbrains.org.objectweb.asm.Opcodes

/** Basic information about a class (e.g., [classId], [kotlinClassHeader] and [supertypes]). */
class BasicClassInfo(
    val classId: ClassId,
    val kotlinClassHeader: KotlinClassHeader?, // null if this is not a Kotlin class
    val supertypes: List<JvmClassName>,

    private val accessFlags: Int,
    val isAnonymous: Boolean
) {
    val isKotlinClass = kotlinClassHeader != null
    val isPrivate = flagEnabled(accessFlags, Opcodes.ACC_PRIVATE)
    val isLocal = classId.isLocal

    val isSynthetic = if (isKotlinClass) {
        // Note that this property is `true` if one of the two checks below is `true`.
        // For example, `kotlin/Metadata.DefaultImpls` is synthetic according to its [KotlinClassHeader.Kind], but not synthetic according
        // to its [accessFlags]. (It's unclear if there is an opposite example.)
        (kotlinClassHeader!!.kind == KotlinClassHeader.Kind.SYNTHETIC_CLASS) || flagEnabled(accessFlags, Opcodes.ACC_SYNTHETIC)
    } else {
        flagEnabled(accessFlags, Opcodes.ACC_SYNTHETIC)
    }

    private fun flagEnabled(accessFlags: Int, flagToCheck: Int) = (accessFlags and flagToCheck) != 0

    companion object {

        fun compute(classContents: ByteArray): BasicClassInfo {
            val kotlinClassHeaderClassVisitor = KotlinClassHeaderClassVisitor()
            val innerClassesClassVisitor = InnerClassesClassVisitor(kotlinClassHeaderClassVisitor)
            val basicClassInfoVisitor = BasicClassInfoClassVisitor(innerClassesClassVisitor)

            // parsingOptions = (SKIP_CODE, SKIP_DEBUG) as method bodies and debug info are not important
            ClassReader(classContents).accept(basicClassInfoVisitor, SKIP_CODE or SKIP_DEBUG)

            val className = basicClassInfoVisitor.getClassName()
            val innerClassesInfo = innerClassesClassVisitor.getInnerClassesInfo()

            return BasicClassInfo(
                classId = resolveNameByInternalName(className, innerClassesInfo),
                kotlinClassHeader = kotlinClassHeaderClassVisitor.getKotlinClassHeader(),
                supertypes = basicClassInfoVisitor.getSupertypes(),
                accessFlags = basicClassInfoVisitor.getAccessFlags(),
                isAnonymous = innerClassesInfo[className]?.let { it.innerSimpleName == null } ?: false
            )
        }
    }
}

private class BasicClassInfoClassVisitor(cv: ClassVisitor) : ClassVisitor(Opcodes.API_VERSION, cv) {
    private var className: String? = null
    private var classAccess: Int? = null
    private val supertypeNames = mutableListOf<String>()

    override fun visit(version: Int, access: Int, name: String, signature: String?, superName: String?, interfaces: Array<String>?) {
        className = name
        classAccess = access
        superName?.let { supertypeNames.add(it) }
        interfaces?.let { supertypeNames.addAll(it) }
        super.visit(version, access, name, signature, superName, interfaces)
    }

    fun getClassName(): String = className!!
    fun getAccessFlags(): Int = classAccess!!
    fun getSupertypes(): List<JvmClassName> = supertypeNames.map { JvmClassName.byInternalName(it) }
}

private class InnerClassesClassVisitor(cv: ClassVisitor) : ClassVisitor(Opcodes.API_VERSION, cv) {

    private val innerClassesInfo = InnerClassesInfo()

    override fun visitInnerClass(name: String, outerName: String?, innerName: String?, access: Int) {
        innerClassesInfo.add(name, outerName, innerName)
        super.visitInnerClass(name, outerName, innerName, access)
    }

    fun getInnerClassesInfo(): InnerClassesInfo = innerClassesInfo
}

private class KotlinClassHeaderClassVisitor : ClassVisitor(Opcodes.API_VERSION) {

    private val kotlinClassHeaderAnnotationVisitor = ReadKotlinClassHeaderAnnotationVisitor()

    override fun visitAnnotation(descriptor: String, visible: Boolean): AnnotationVisitor? {
        return convertAnnotationVisitor(
            kotlinClassHeaderAnnotationVisitor,
            descriptor,
            InnerClassesInfo() // This info is not needed to resolve KotlinClassHeader
        )
    }

    fun getKotlinClassHeader(): KotlinClassHeader? = kotlinClassHeaderAnnotationVisitor.createHeaderWithDefaultMetadataVersion()
}
