/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

@file:Suppress("unused") // The declarations below may be used outside the Kotlin repo

package org.jetbrains.kotlin.serialization.konan.impl

@Deprecated(
    "This class has been moved from package org.jetbrains.kotlin.serialization.konan.impl to package org.jetbrains.kotlin.library.metadata.impl",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.impl.KlibMetadataModuleDescriptorFactoryImpl")
)
typealias KlibMetadataModuleDescriptorFactoryImpl = org.jetbrains.kotlin.library.metadata.impl.KlibMetadataModuleDescriptorFactoryImpl

@Deprecated(
    "This class has been moved from package org.jetbrains.kotlin.serialization.konan.impl to package org.jetbrains.kotlin.library.metadata.impl",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.impl.KlibResolvedModuleDescriptorsFactoryImpl")
)
typealias KlibResolvedModuleDescriptorsFactoryImpl = org.jetbrains.kotlin.library.metadata.impl.KlibResolvedModuleDescriptorsFactoryImpl

@Deprecated(
    "This class has been moved from package org.jetbrains.kotlin.serialization.konan.impl to package org.jetbrains.kotlin.library.metadata.impl",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.impl.ForwardDeclarationsPackageFragmentDescriptor")
)
typealias ForwardDeclarationsPackageFragmentDescriptor = org.jetbrains.kotlin.library.metadata.impl.ForwardDeclarationsPackageFragmentDescriptor

@Deprecated(
    "This object has been moved from package org.jetbrains.kotlin.serialization.konan.impl to package org.jetbrains.kotlin.library.metadata.impl",
    ReplaceWith("org.jetbrains.kotlin.library.metadata.impl.ForwardDeclarationsFqNames")
)
@Suppress("DEPRECATION_ERROR")
typealias ForwardDeclarationsFqNames = org.jetbrains.kotlin.library.metadata.impl.ForwardDeclarationsFqNames
