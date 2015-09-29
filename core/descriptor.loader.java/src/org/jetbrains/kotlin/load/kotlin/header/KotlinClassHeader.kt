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

import org.jetbrains.kotlin.serialization.deserialization.BinaryVersion
import org.jetbrains.kotlin.load.java.AbiVersionUtil
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.KotlinClass
import org.jetbrains.kotlin.load.java.JvmAnnotationNames.KotlinSyntheticClass

public class KotlinClassHeader(
        public val kind: KotlinClassHeader.Kind,
        public val version: BinaryVersion,
        public val annotationData: Array<String>?,
        public val strings: Array<String>?,
        public val classKind: KotlinClass.Kind?,
        public val syntheticClassKind: KotlinSyntheticClass.Kind?,
        public val filePartClassNames: Array<String>?,
        public val multifileClassName: String?
) {
    public val isCompatibleAbiVersion: Boolean get() = AbiVersionUtil.isAbiVersionCompatible(version)

    public enum class Kind {
        CLASS,
        PACKAGE_FACADE,
        FILE_FACADE,
        MULTIFILE_CLASS,
        MULTIFILE_CLASS_PART,
        SYNTHETIC_CLASS
    }

    override fun toString() =
            "$kind " +
            (if (classKind != null) "$classKind " else "") +
            (if (syntheticClassKind != null) "$syntheticClassKind " else "") +
            "version=$version"
}

public fun KotlinClassHeader.isCompatibleClassKind(): Boolean = isCompatibleAbiVersion && kind == KotlinClassHeader.Kind.CLASS
public fun KotlinClassHeader.isCompatiblePackageFacadeKind(): Boolean = isCompatibleAbiVersion && kind == KotlinClassHeader.Kind.PACKAGE_FACADE
public fun KotlinClassHeader.isCompatibleFileFacadeKind(): Boolean = isCompatibleAbiVersion && kind == KotlinClassHeader.Kind.FILE_FACADE
public fun KotlinClassHeader.isCompatibleMultifileClassKind(): Boolean = isCompatibleAbiVersion && kind == KotlinClassHeader.Kind.MULTIFILE_CLASS
public fun KotlinClassHeader.isCompatibleMultifileClassPartKind(): Boolean = isCompatibleAbiVersion && kind == KotlinClassHeader.Kind.MULTIFILE_CLASS_PART
public fun KotlinClassHeader.isCompatibleSyntheticClassKind(): Boolean = isCompatibleAbiVersion && kind == KotlinClassHeader.Kind.SYNTHETIC_CLASS
