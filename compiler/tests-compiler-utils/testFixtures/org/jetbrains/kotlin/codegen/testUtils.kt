/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen

import java.net.URL
import java.net.URLClassLoader


fun ClassLoader?.extractUrls(): List<URL> {
    return (this as? URLClassLoader)?.let {
        it.urLs.toList() + it.parent.extractUrls()
    } ?: emptyList()
}
