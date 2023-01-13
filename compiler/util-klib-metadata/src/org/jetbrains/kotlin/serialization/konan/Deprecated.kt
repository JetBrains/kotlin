/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused") // The declarations below may be used outside the Kotlin repo

package org.jetbrains.kotlin.serialization.konan

@Deprecated(
    "This interface has been moved from package org.jetbrains.kotlin.serialization.konan to package org.jetbrains.kotlin.library.metadata",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.KlibResolvedModuleDescriptorsFactory")
)
typealias KlibResolvedModuleDescriptorsFactory = org.jetbrains.kotlin.library.metadata.KlibResolvedModuleDescriptorsFactory

@Deprecated(
    "This class has been moved from package org.jetbrains.kotlin.serialization.konan to package org.jetbrains.kotlin.library.metadata",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.KotlinResolvedModuleDescriptors")
)
typealias KotlinResolvedModuleDescriptors = org.jetbrains.kotlin.library.metadata.KotlinResolvedModuleDescriptors
