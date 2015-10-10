/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.load.kotlin.header

import org.jetbrains.kotlin.load.java.AbiVersionUtil
import org.jetbrains.kotlin.serialization.deserialization.BinaryVersion

class KotlinClassHeader(
        val kind: KotlinClassHeader.Kind,
        val version: BinaryVersion,
        val annotationData: Array<String>?,
        val strings: Array<String>?,
        val syntheticClassKind: String?,
        val filePartClassNames: Array<String>?,
        val multifileClassName: String?,
        val isInterfaceDefaultImpls: Boolean,
        val isLocalClass: Boolean
) {
    val isCompatibleAbiVersion: Boolean get() = AbiVersionUtil.isAbiVersionCompatible(version)

    enum class Kind {
        CLASS,
        PACKAGE_FACADE,
        FILE_FACADE,
        MULTIFILE_CLASS,
        MULTIFILE_CLASS_PART,
        SYNTHETIC_CLASS
    }

    override fun toString() =
            "$kind " +
            (if (isLocalClass) "(local) " else "") +
            (if (syntheticClassKind != null) "$syntheticClassKind " else "") +
            "version=$version"
}

fun KotlinClassHeader.isCompatibleClassKind(): Boolean = isCompatibleAbiVersion && kind == KotlinClassHeader.Kind.CLASS
fun KotlinClassHeader.isCompatiblePackageFacadeKind(): Boolean = isCompatibleAbiVersion && kind == KotlinClassHeader.Kind.PACKAGE_FACADE
fun KotlinClassHeader.isCompatibleFileFacadeKind(): Boolean = isCompatibleAbiVersion && kind == KotlinClassHeader.Kind.FILE_FACADE
fun KotlinClassHeader.isCompatibleMultifileClassKind(): Boolean = isCompatibleAbiVersion && kind == KotlinClassHeader.Kind.MULTIFILE_CLASS
fun KotlinClassHeader.isCompatibleMultifileClassPartKind(): Boolean = isCompatibleAbiVersion && kind == KotlinClassHeader.Kind.MULTIFILE_CLASS_PART
