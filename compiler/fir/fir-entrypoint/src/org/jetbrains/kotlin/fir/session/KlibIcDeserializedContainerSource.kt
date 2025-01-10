/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.session

import org.jetbrains.kotlin.descriptors.SourceFile
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.serialization.deserialization.IncompatibleVersionErrorData
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerAbiStability
import org.jetbrains.kotlin.serialization.deserialization.descriptors.DeserializedContainerSource

class KlibIcDeserializedContainerSource(packageFqName: FqName) : DeserializedContainerSource {
    override val presentableString: String = "Package '$packageFqName'"
    override val incompatibility: IncompatibleVersionErrorData<*>? get() = null
    override val isPreReleaseInvisible: Boolean get() = false
    override val abiStability: DeserializedContainerAbiStability get() = DeserializedContainerAbiStability.STABLE
    override fun getContainingFile(): SourceFile = SourceFile.NO_SOURCE_FILE
}