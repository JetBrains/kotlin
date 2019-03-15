/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.deserialization

interface MetadataPartProvider {
    /**
     * @return simple names of .kotlin_metadata files that store data for top level declarations in the package with the given FQ name
     */
    fun findMetadataPackageParts(packageFqName: String): List<String>

    object Empty : MetadataPartProvider {
        override fun findMetadataPackageParts(packageFqName: String): List<String> = emptyList()
    }
}
