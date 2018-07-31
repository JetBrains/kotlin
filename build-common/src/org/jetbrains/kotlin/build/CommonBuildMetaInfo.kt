/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion

/**
 * If you want to add a new field, check its type is supported by [serializeToPlainText], [deserializeFromPlainText]
 */
data class CommonBuildMetaInfo(
    override val isEAP: Boolean,
    override val compilerBuildVersion: String,
    override val languageVersionString: String,
    override val apiVersionString: String,
    override val coroutinesEnable: Boolean,
    override val coroutinesWarn: Boolean,
    override val coroutinesError: Boolean,
    override val multiplatformEnable: Boolean,
    override val metadataVersionMajor: Int,
    override val metadataVersionMinor: Int,
    override val metadataVersionPatch: Int,
    override val ownVersion: Int,
    override val coroutinesVersion: Int,
    override val multiplatformVersion: Int
) : BuildMetaInfo {
    companion object : BuildMetaInfoFactory<CommonBuildMetaInfo>(CommonBuildMetaInfo::class) {
        override fun create(
            isEAP: Boolean,
            compilerBuildVersion: String,
            languageVersionString: String,
            apiVersionString: String,
            coroutinesEnable: Boolean,
            coroutinesWarn: Boolean,
            coroutinesError: Boolean,
            multiplatformEnable: Boolean,
            ownVersion: Int,
            coroutinesVersion: Int,
            multiplatformVersion: Int,
            metadataVersionArray: IntArray?
        ): CommonBuildMetaInfo {
            val metadataVersion = metadataVersionArray?.let(::JvmMetadataVersion) ?: JvmMetadataVersion.INSTANCE
            return CommonBuildMetaInfo(
                isEAP = isEAP,
                compilerBuildVersion = compilerBuildVersion,
                languageVersionString = languageVersionString,
                apiVersionString = apiVersionString,
                coroutinesEnable = coroutinesEnable,
                coroutinesWarn = coroutinesWarn,
                coroutinesError = coroutinesError,
                multiplatformEnable = multiplatformEnable,
                metadataVersionMajor = metadataVersion.major,
                metadataVersionMinor = metadataVersion.minor,
                metadataVersionPatch = metadataVersion.patch,
                ownVersion = ownVersion,
                coroutinesVersion = coroutinesVersion,
                multiplatformVersion = multiplatformVersion
            )
        }
    }
}
