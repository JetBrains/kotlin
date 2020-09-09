/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.load.kotlin

import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.metadata.ProtoBuf
import org.jetbrains.kotlin.metadata.deserialization.NameResolver
import org.jetbrains.kotlin.metadata.deserialization.getExtensionOrNull
import org.jetbrains.kotlin.metadata.jvm.JvmProtoBuf
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmProtoBufUtil
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class JvmPackagePartSource(
    val className: JvmClassName,
    val facadeClassName: JvmClassName?,
    packageProto: ProtoBuf.Package,
    nameResolver: NameResolver,
    override val incompatibility: IncompatibleVersionErrorData<JvmMetadataVersion>? = null,
    override val isPreReleaseInvisible: Boolean = false,
    override val isInvisibleIrDependency: Boolean = false,
    val knownJvmBinaryClass: KotlinJvmBinaryClass? = null
) : DeserializedContainerSource {
    constructor(
        kotlinClass: KotlinJvmBinaryClass,
        packageProto: ProtoBuf.Package,
        nameResolver: NameResolver,
        incompatibility: IncompatibleVersionErrorData<JvmMetadataVersion>? = null,
        isPreReleaseInvisible: Boolean = false,
        isInvisibleIrDependency: Boolean = false
    ) : this(
        JvmClassName.byClassId(kotlinClass.classId),
        kotlinClass.classHeader.multifileClassName?.let {
            if (it.isNotEmpty()) JvmClassName.byInternalName(it) else null
        },
        packageProto,
        nameResolver,
        incompatibility,
        isPreReleaseInvisible,
        isInvisibleIrDependency,
        kotlinClass
    )

    val moduleName =
        packageProto.getExtensionOrNull(JvmProtoBuf.packageModuleName)?.let(nameResolver::getString)
            ?: JvmProtoBufUtil.DEFAULT_MODULE_NAME

    override val presentableString: String
        get() = "Class '${classId.asSingleFqName().asString()}'"

    val simpleName: Name get() = Name.identifier(className.internalName.substringAfterLast('/'))

    val classId: ClassId get() = ClassId(className.packageFqName, simpleName)

    override fun toString() = "${this::class.java.simpleName}: $className"

    override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE
}
