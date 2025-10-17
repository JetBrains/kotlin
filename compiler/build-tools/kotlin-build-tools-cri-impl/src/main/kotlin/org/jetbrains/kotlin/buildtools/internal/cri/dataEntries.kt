/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:OptIn(ExperimentalSerializationApi::class, ExperimentalBuildToolsApi::class)

package org.jetbrains.kotlin.buildtools.internal.cri

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.protobuf.ProtoNumber
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.buildtools.api.cri.FileIdToPathEntry
import org.jetbrains.kotlin.buildtools.api.cri.LookupEntry
import org.jetbrains.kotlin.buildtools.api.cri.SubtypeEntry

@Serializable
internal data class LookupEntryImpl(
    @ProtoNumber(1) override val key: Long?,
    @ProtoNumber(2) override val fileIds: List<Int>,
) : LookupEntry

@Serializable
internal data class FileIdToPathEntryImpl(
    @ProtoNumber(1) override val fileId: Int?,
    @ProtoNumber(2) override val path: String?,
) : FileIdToPathEntry

@Serializable
internal data class SubtypeEntryImpl(
    @ProtoNumber(1) override val className: String?,
    @ProtoNumber(2) override val subtypes: List<String>,
) : SubtypeEntry
