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
