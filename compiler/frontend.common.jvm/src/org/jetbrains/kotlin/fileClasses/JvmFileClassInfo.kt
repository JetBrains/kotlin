/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
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

