/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.metadata.deserialization

interface NameResolver {
    fun getString(index: Int): String

    /**
     * @return the fully qualified name of some class in the format: `org/foo/bar/Test.Inner`
     */
    fun getQualifiedClassName(index: Int): String

    fun isLocalClassName(index: Int): Boolean
}
