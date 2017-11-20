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

@file:JvmName("FileClasses")

package org.jetbrains.kotlin.fileClasses

import org.jetbrains.kotlin.psi.KtFile

@Deprecated("Use JvmFileClassUtil instead.", level = DeprecationLevel.ERROR)
open class JvmFileClassesProvider

@Suppress("DEPRECATION_ERROR")
@Deprecated("Use JvmFileClassUtil instead.", level = DeprecationLevel.ERROR)
object NoResolveFileClassesProvider : JvmFileClassesProvider()

@Suppress("DEPRECATION_ERROR", "unused")
@Deprecated("Use JvmFileClassUtil.getFileClassInternalName instead.", ReplaceWith("JvmFileClassUtil.getFileClassInternalName(file)"), DeprecationLevel.ERROR)
fun JvmFileClassesProvider.getFileClassInternalName(file: KtFile): String =
        JvmFileClassUtil.getFileClassInternalName(file)
