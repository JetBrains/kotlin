/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.build

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.metadata.deserialization.BinaryVersion
import org.jetbrains.kotlin.metadata.deserialization.MetadataVersion

class CommonBuildMetaInfo : BuildMetaInfo() {
    override fun checkIfPlatformSpecificCompilerArgumentWasChanged(key: String, currentValue: String, previousValue: String): Boolean? {
        when (key) {
            CustomKeys.METADATA_VERSION_STRING.name -> {
                val currentVersionIntArray = BinaryVersion.parseVersionArray(currentValue)
                if (currentVersionIntArray?.size != 3) return null
                val currentVersion = MetadataVersion(currentVersionIntArray[0], currentVersionIntArray[1], currentVersionIntArray[2])

                val previousVersionIntArray = BinaryVersion.parseVersionArray(previousValue)
                if (previousVersionIntArray?.size != 3) return null
                val previousVersion = MetadataVersion(previousVersionIntArray[0], previousVersionIntArray[1], previousVersionIntArray[2])
                return currentVersion == previousVersion
            }
        }
        return null
    }

    override fun createPropertiesMapFromCompilerArguments(args: CommonCompilerArguments, additionalArguments: String): Map<String, String> {
        val resultMap = mutableMapOf<String, String>()
        val metadataVersionArray = args.metadataVersion?.let { BinaryVersion.parseVersionArray(it) }
        val metadataVersion = metadataVersionArray?.let(::MetadataVersion) ?: MetadataVersion.INSTANCE
        val metadataVersionString = metadataVersion.toString()
        resultMap[CustomKeys.METADATA_VERSION_STRING.name] = metadataVersionString

        return super.createPropertiesMapFromCompilerArguments(args, additionalArguments) + resultMap
    }
}