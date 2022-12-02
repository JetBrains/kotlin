/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub.file

import com.intellij.openapi.diagnostic.Logger
import org.jetbrains.kotlin.load.kotlin.KotlinJvmBinarySourceElement
import org.jetbrains.kotlin.load.kotlin.findKotlinClass
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.serialization.deserialization.ClassData
import org.jetbrains.kotlin.serialization.deserialization.ClassDataFinder

class DirectoryBasedDataFinder(
    val classFinder: DirectoryBasedClassFinder,
    val log: Logger,
    private val jvmMetadataVersion: JvmMetadataVersion
) : ClassDataFinder {
    override fun findClassData(classId: ClassId): ClassData? {
        val binaryClass = classFinder.findKotlinClass(classId, jvmMetadataVersion) ?: return null
        val classHeader = binaryClass.classHeader
        val data = classHeader.data
        if (data == null) {
            log.error("Annotation data missing for ${binaryClass.classId}")
            return null
        }
        val strings = classHeader.strings
        if (strings == null) {
            log.error("String table not found in class ${binaryClass.classId}")
            return null
        }

        val (nameResolver, classProto) = JvmProtoBufUtil.readClassDataFrom(data, strings)
        return ClassData(nameResolver, classProto, classHeader.metadataVersion, KotlinJvmBinarySourceElement(binaryClass))
    }
}