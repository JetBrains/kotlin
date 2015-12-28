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

import org.jetbrains.kotlin.load.java.JvmBytecodeBinaryVersion
import org.jetbrains.kotlin.load.kotlin.JvmMetadataVersion

class KotlinClassHeader(
        val kind: KotlinClassHeader.Kind,
        val metadataVersion: JvmMetadataVersion,
        val bytecodeVersion: JvmBytecodeBinaryVersion,
        val annotationData: Array<String>?,
        val strings: Array<String>?,
        val filePartClassNames: Array<String>?,
        val multifileClassName: String?,
        val isInterfaceDefaultImpls: Boolean,
        val isLocalClass: Boolean
) {
    enum class Kind {
        CLASS,
        FILE_FACADE,
        MULTIFILE_CLASS,
        MULTIFILE_CLASS_PART,
        SYNTHETIC_CLASS
    }

    override fun toString() = "$kind " + (if (isLocalClass) "(local) " else "") + "version=$metadataVersion"
}
