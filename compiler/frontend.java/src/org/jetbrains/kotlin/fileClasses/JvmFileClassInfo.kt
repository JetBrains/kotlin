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

package org.jetbrains.kotlin.fileClasses

import org.jetbrains.kotlin.name.FqName

interface JvmFileClassInfo {
    val fileClassFqName: FqName
    val facadeClassFqName: FqName
    val withJvmName: Boolean
    val withJvmMultifileClass: Boolean
}

class JvmSimpleFileClassInfo(
        override val fileClassFqName: FqName,
        override val withJvmName: Boolean
) : JvmFileClassInfo {
    override val facadeClassFqName: FqName get() = fileClassFqName
    override val withJvmMultifileClass: Boolean get() = false
}

class JvmMultifileClassPartInfo(
        override val fileClassFqName: FqName,
        override val facadeClassFqName: FqName
) : JvmFileClassInfo {
    override val withJvmName: Boolean get() = true
    override val withJvmMultifileClass: Boolean get() = true
}

