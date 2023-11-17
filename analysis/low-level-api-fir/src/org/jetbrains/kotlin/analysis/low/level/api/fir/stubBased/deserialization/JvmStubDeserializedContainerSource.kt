/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerAbiStability
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

//required for LLFirDependenciesSymbolProvider#jvmClassName, to resolve ambiguities
//todo check if moving builtins to stubs would solve the issue
internal class JvmStubDeserializedContainerSource(classId: ClassId) : DeserializedContainerSourceWithJvmClassName {
    override val className = JvmClassName.byClassId(classId)

    override val incompatibility: IncompatibleVersionErrorData<*>?
        get() = null

    override val isPreReleaseInvisible: Boolean
        get() = false

    override val abiStability: DeserializedContainerAbiStability
        get() = DeserializedContainerAbiStability.STABLE

    override val presentableString: String
        get() = className.internalName

    override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE
}