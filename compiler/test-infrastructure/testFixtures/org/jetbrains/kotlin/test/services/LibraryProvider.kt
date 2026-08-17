/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.library.KotlinLibrary

class LibraryProvider(private val testServices: TestServices) : TestService {
    private val descriptorToLibrary = mutableMapOf<ModuleDescriptor, KotlinLibrary>()
    private val stdlibPathToDescriptor = mutableMapOf<String, ModuleDescriptor>()

    fun getOrCreateStdlibByPath(path: String, create: (String) -> Pair<ModuleDescriptor, KotlinLibrary>): ModuleDescriptor {
        return stdlibPathToDescriptor.getOrPut(path) {
            create(path).let {
                descriptorToLibrary += it
                it.first
            }
        }
    }
}

val TestServices.libraryProvider: LibraryProvider by TestServices.testServiceAccessor()
