/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.low.level.api.fir.stubBased.deserialization

import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.resolve.jvm.JvmClassName
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.serialization.deserialization.builtins.BuiltInSerializerProtocol
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerAbiStability
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

/**
 * Container source for deserialized declarations from ".kotlin_builtins" file.
 *
 * [facadeClassName] points to a facade class (e.g. "kotlin/LibraryKt") and
 * is used to differentiate between different builtins files.
 *
 * We have a dedicated container source for such declaration because we don't want
 * them to be identified as belonging to a facade class.
 *
 * For that specific reason, this class **DOES NOT** implement
 * [org.jetbrains.kotlin.load.kotlin.FacadeClassSource],
 * because compiler backend might use instance checks to detect
 * regular facade files.
 *
 * See for KTIJ-27124 for an example of an issue in IR lowerings.
 */
internal class JvmStubDeserializedBuiltInsContainerSource(val facadeClassName: JvmClassName) : DeserializedContainerSource {
    override val incompatibility: IncompatibleVersionErrorData<*>?
        get() = null

    override val isPreReleaseInvisible: Boolean
        get() = false

    override val abiStability: DeserializedContainerAbiStability
        get() = DeserializedContainerAbiStability.STABLE

    override val presentableString: String
        get() = "Declarations from ${BuiltInSerializerProtocol.BUILTINS_FILE_EXTENSION} file '${facadeClassName}'"

    override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE
}
