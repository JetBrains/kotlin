/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea

import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.Key
import org.jetbrains.jps.model.module.JpsModuleSourceRootType

// WARNING, this API is used by AS3.3+


const val LIBRARY_KEY_NAME = "Kt_Library"
const val SDK_KEY_NAME = "Kt_Sdk"
const val MODULE_ROOT_TYPE_KEY_NAME = "Kt_SourceRootType"

@JvmField
val MODULE_ROOT_TYPE_KEY = getOrCreateKey<JpsModuleSourceRootType<*>>(MODULE_ROOT_TYPE_KEY_NAME)

@JvmField
val SDK_KEY = getOrCreateKey<Sdk>(SDK_KEY_NAME)

@JvmField
val LIBRARY_KEY = getOrCreateKey<Library>(LIBRARY_KEY_NAME)


inline fun <reified T> getOrCreateKey(name: String): Key<T> {
    @Suppress("DEPRECATION", "UNCHECKED_CAST")
    val existingKey = Key.findKeyByName(name) as Key<T>?
    return existingKey ?: Key.create<T>(name)
}
