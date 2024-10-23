/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.diagnostics

enum class JvmDeclarationOriginKind {
    OTHER,
    PACKAGE_PART,
    DELEGATION,
    INLINE_VERSION_OF_SUSPEND_FUN,
}
