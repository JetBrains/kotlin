/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.extensions

/**
 * Class representing an extension point. Used as a key in structures which store
 * specific instances of extensions.
 */
abstract class ExtensionPointDescriptor<T : Any>(val name: String, val extensionClass: Class<T>)
