/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.serialization

import java.io.OutputStream

interface StringTable {
    fun getStringIndex(string: String): Int

    /**
     * @param className the fully qualified name of some class in the format: `org/foo/bar/Test.Inner`
     */
    fun getQualifiedClassNameIndex(className: String, isLocal: Boolean): Int

    fun serializeTo(output: OutputStream)
}
