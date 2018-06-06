/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.build

import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.config.KotlinCompilerVersion
import org.jetbrains.kotlin.config.LanguageVersion
import org.jetbrains.kotlin.config.isPreRelease
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmBytecodeBinaryVersion
import org.jetbrains.kotlin.metadata.jvm.deserialization.JvmMetadataVersion

/**
 * If you want to add a new field, check its type is supported by [serializeToPlainText], [deserializeFromPlainText]
 */
data class JvmBuildMetaInfo(
        val isEAP: Boolean,
        val compilerBuildVersion: String,
        val languageVersionString: String,
        val apiVersionString: String,
        val coroutinesEnable: Boolean,
        val coroutinesWarn: Boolean,
        val coroutinesError: Boolean,
        val multiplatformEnable: Boolean,
        val metadataVersionMajor: Int,
        val metadataVersionMinor: Int,
        val metadataVersionPatch: Int,
        val bytecodeVersionMajor: Int,
        val bytecodeVersionMinor: Int,
        val bytecodeVersionPatch: Int,
        val ownVersion: Int = JvmBuildMetaInfo.OWN_VERSION,
        val coroutinesVersion: Int = JvmBuildMetaInfo.COROUTINES_VERSION,
        val multiplatformVersion: Int = JvmBuildMetaInfo.MULTIPLATFORM_VERSION
) {
    companion object {
        const val OWN_VERSION: Int = 0
        const val COROUTINES_VERSION: Int = 0
        const val MULTIPLATFORM_VERSION: Int = 0

        fun serializeToString(info: JvmBuildMetaInfo): String =
                serializeToPlainText(info)

        fun deserializeFromString(str: String): JvmBuildMetaInfo? =
                deserializeFromPlainText(str)
    }
}

fun JvmBuildMetaInfo(args: CommonCompilerArguments): JvmBuildMetaInfo {
    val languageVersion = args.languageVersion?.let((LanguageVersion)::fromVersionString) ?: LanguageVersion.LATEST_STABLE

    return JvmBuildMetaInfo(
            isEAP = languageVersion.isPreRelease(),
            compilerBuildVersion = KotlinCompilerVersion.VERSION,
            languageVersionString = languageVersion.versionString,
            apiVersionString = args.apiVersion ?: languageVersion.versionString,
            coroutinesEnable = args.coroutinesState == CommonCompilerArguments.ENABLE,
            coroutinesWarn = args.coroutinesState == CommonCompilerArguments.WARN,
            coroutinesError = args.coroutinesState == CommonCompilerArguments.ERROR,
            multiplatformEnable = args.multiPlatform,
            metadataVersionMajor = JvmMetadataVersion.INSTANCE.major,
            metadataVersionMinor = JvmMetadataVersion.INSTANCE.minor,
            metadataVersionPatch = JvmMetadataVersion.INSTANCE.patch,
            bytecodeVersionMajor = JvmBytecodeBinaryVersion.INSTANCE.major,
            bytecodeVersionMinor = JvmBytecodeBinaryVersion.INSTANCE.minor,
            bytecodeVersionPatch = JvmBytecodeBinaryVersion.INSTANCE.patch
    )
}
