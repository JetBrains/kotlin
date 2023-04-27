/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.load.kotlin.FacadeClassSource
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerAbiStability
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

//required for LLFirDependenciesSymbolProvider#jvmClassName, to resolve ambiguities
//todo check if moving builtins to stubs would solve the issue
internal class JvmFromStubDecompilerSource(
    override val className: JvmClassName,
    override val facadeClassName: JvmClassName? = null,
    override val incompatibility: IncompatibleVersionErrorData<JvmMetadataVersion>? = null,
    override val isPreReleaseInvisible: Boolean = false,
    override val abiStability: DeserializedContainerAbiStability = DeserializedContainerAbiStability.STABLE,
) : DeserializedContainerSource, FacadeClassSource {
    constructor(packageName: FqName) :
            this(JvmClassName.byClassId(ClassId.topLevel(JvmClassName.byInternalName(packageName.asString()).fqNameForTopLevelClassMaybeWithDollars)))

    override fun getContainingFile(): SourceFile {
        return SourceFile.NO_SOURCE_FILE
    }

    override val presentableString: String
        get() = className.internalName
}