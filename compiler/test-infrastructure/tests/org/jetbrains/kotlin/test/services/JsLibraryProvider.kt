/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.test.services

import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.library.KotlinLibrary

class JsLibraryProvider(private val testServices: TestServices) : TestService {
    private val descriptorToLibrary = mutableMapOf<ModuleDescriptor, KotlinLibrary>()
    private val stdlibPathToDescriptor = mutableMapOf<String, ModuleDescriptorImpl>()

    fun getDescriptorByPath(path: String): ModuleDescriptorImpl {
        return stdlibPathToDescriptor[path] ?: testServices.assertions.fail {
            "There is no library with path $path"
        }
    }

    fun setDescriptorAndLibraryByName(name: String, descriptor: ModuleDescriptorImpl, library: KotlinLibrary) {
        stdlibPathToDescriptor[name] = descriptor
        descriptorToLibrary[descriptor] = library
    }

    fun getCompiledLibraryByDescriptor(descriptor: ModuleDescriptor): KotlinLibrary {
        return descriptorToLibrary[descriptor] ?: testServices.assertions.fail {
            "There is no library for descriptor ${descriptor.name}"
        }
    }

    fun getPathByDescriptor(descriptor: ModuleDescriptor): String {
        return stdlibPathToDescriptor.entries.single { it.value == descriptor }.key
    }

    fun getDescriptorByCompiledLibrary(library: KotlinLibrary): ModuleDescriptor {
        return descriptorToLibrary.filterValues { it == library }.keys.singleOrNull() ?: testServices.assertions.fail {
            "There is no descriptor for library ${library.libraryName}"
        }
    }

    fun getOrCreateStdlibByPath(path: String, create: (String) -> Pair<ModuleDescriptorImpl, KotlinLibrary>): ModuleDescriptorImpl {
        return stdlibPathToDescriptor.getOrPut(path) {
            create(path).let {
                descriptorToLibrary += it
                it.first
            }
        }
    }
}

val TestServices.jsLibraryProvider: JsLibraryProvider by TestServices.testServiceAccessor()
