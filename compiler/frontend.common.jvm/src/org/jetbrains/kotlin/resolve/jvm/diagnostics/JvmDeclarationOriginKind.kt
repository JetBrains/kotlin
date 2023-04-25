/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.diagnostics

enum class JvmDeclarationOriginKind {
    OTHER,
    PACKAGE_PART,
    INTERFACE_DEFAULT_IMPL,
    CLASS_MEMBER_DELEGATION_TO_DEFAULT_IMPL,
    DEFAULT_IMPL_DELEGATION_TO_SUPERINTERFACE_DEFAULT_IMPL,
    DELEGATION,
    SAM_DELEGATION,
    BRIDGE,
    MULTIFILE_CLASS,
    MULTIFILE_CLASS_PART,
    SYNTHETIC, // this means that there's no proper descriptor for this jvm declaration,
    COLLECTION_STUB,
    AUGMENTED_BUILTIN_API,
    UNBOX_METHOD_OF_INLINE_CLASS,
    JVM_OVERLOADS,
    INLINE_VERSION_OF_SUSPEND_FUN,
}
