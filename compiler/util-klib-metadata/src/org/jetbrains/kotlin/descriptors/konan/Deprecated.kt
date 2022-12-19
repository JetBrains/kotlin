/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused") // The declarations below may be used outside the Kotlin repo

package org.jetbrains.kotlin.descriptors.konan

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.metadata.klibModuleOrigin
import org.jetbrains.kotlin.library.metadata.kotlinLibrary

@Deprecated(
    "This interface has been moved from package org.jetbrains.kotlin.descriptors.konan to package org.jetbrains.kotlin.library.metadata",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.KlibModuleDescriptorFactory")
)
typealias KlibModuleDescriptorFactory = org.jetbrains.kotlin.library.metadata.KlibModuleDescriptorFactory

@Deprecated(
    "This class has been moved from package org.jetbrains.kotlin.descriptors.konan to package org.jetbrains.kotlin.library.metadata",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.KlibModuleOrigin")
)
typealias KlibModuleOrigin = org.jetbrains.kotlin.library.metadata.KlibModuleOrigin

@Deprecated(
    "This class has been moved from package org.jetbrains.kotlin.descriptors.konan to package org.jetbrains.kotlin.library.metadata",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.CompiledKlibModuleOrigin")
)
typealias CompiledKlibModuleOrigin = org.jetbrains.kotlin.library.metadata.CompiledKlibModuleOrigin

@Deprecated(
    "This class has been moved from package org.jetbrains.kotlin.descriptors.konan to package org.jetbrains.kotlin.library.metadata",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin")
)
typealias DeserializedKlibModuleOrigin = org.jetbrains.kotlin.library.metadata.DeserializedKlibModuleOrigin

@Deprecated(
    "This object has been moved from package org.jetbrains.kotlin.descriptors.konan to package org.jetbrains.kotlin.library.metadata",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.CurrentKlibModuleOrigin")
)
typealias CurrentKlibModuleOrigin = org.jetbrains.kotlin.library.metadata.CurrentKlibModuleOrigin

@Deprecated(
    "This object has been moved from package org.jetbrains.kotlin.descriptors.konan to package org.jetbrains.kotlin.library.metadata",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.SyntheticModulesOrigin")
)
typealias SyntheticModulesOrigin = org.jetbrains.kotlin.library.metadata.SyntheticModulesOrigin

@Deprecated(
    "This property has been moved from package org.jetbrains.kotlin.descriptors.konan to package org.jetbrains.kotlin.library.metadata",
    ReplaceWith("klibModuleOrigin", "org.jetbrains.kotlin.library.metadata.klibModuleOrigin")
)
inline val ModuleDescriptor.klibModuleOrigin: org.jetbrains.kotlin.library.metadata.KlibModuleOrigin
    get() = klibModuleOrigin

@Deprecated(
    "This property has been moved from package org.jetbrains.kotlin.descriptors.konan to package org.jetbrains.kotlin.library.metadata",
    ReplaceWith("kotlinLibrary", "org.jetbrains.kotlin.library.metadata.kotlinLibrary")
)
inline val ModuleDescriptor.kotlinLibrary: KotlinLibrary
    get() = kotlinLibrary
