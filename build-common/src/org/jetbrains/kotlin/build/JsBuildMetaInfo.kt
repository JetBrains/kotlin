/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.incremental.storage.RelativeFileToPathConverter
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.utils.JsMetadataVersion

class JsBuildMetaInfo(converter: RelativeFileToPathConverter?) : BuildMetaInfo(converter) {
    override fun checkIfPlatformSpecificCompilerArgumentWasChanged(key: String, currentValue: String, previousValue: String): Boolean? {
        when (key) {
            CustomKeys.METADATA_VERSION_STRING.name -> {
                val currentValueIntArray = BinaryVersion.parseVersionArray(currentValue)
                if (currentValueIntArray?.size != 3) return null
                val currentVersion = JsMetadataVersion(currentValueIntArray[0], currentValueIntArray[1], currentValueIntArray[2])

                val previousValueIntArray = BinaryVersion.parseVersionArray(previousValue)
                if (previousValueIntArray?.size != 3) return null
                val previousVersion = JsMetadataVersion(previousValueIntArray[0], previousValueIntArray[1], previousValueIntArray[2])
                return currentVersion == previousVersion
            }
        }
        return null
    }

    override fun createPropertiesMapFromCompilerArguments(args: CommonCompilerArguments): Map<String, String> {
        val resultMap = mutableMapOf<String, String>()
        val metadataVersionArray = args.metadataVersion?.let { BinaryVersion.parseVersionArray(it) }
        val metadataVersion = metadataVersionArray?.let(::JsMetadataVersion) ?: JsMetadataVersion.INSTANCE
        val metadataVersionString = metadataVersion.toInteger().toString()
        resultMap[CustomKeys.METADATA_VERSION_STRING.name] = metadataVersionString

        return super.createPropertiesMapFromCompilerArguments(args) + resultMap
    }

    override val argumentsListForSpecialCheck: List<String>
        get() = super.argumentsListForSpecialCheck + listOf("sourceMap", "metaInfo" + "partialLinkage" + "wasmDebug")
}