/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.load.kotlin.FacadeClassSource
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerAbiStability
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

internal class JvmStubDeserializedFacadeContainerSource(
    override val className: JvmClassName,
    override val facadeClassName: JvmClassName?
) : DeserializedContainerSource, FacadeClassSource {
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