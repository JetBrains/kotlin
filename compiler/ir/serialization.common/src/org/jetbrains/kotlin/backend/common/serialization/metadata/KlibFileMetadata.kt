/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.metadata

import org.jetbrains.kotlin.library.metadata.KlibMetadataPackageFragment
import org.jetbrains.kotlin.psi.KtFile

sealed class KlibFileMetadata

data class KotlinPsiFileMetadata(val ktFile: KtFile) : KlibFileMetadata()

data class KotlinDeserializedFileMetadata(
    val packageFragment: KlibMetadataPackageFragment,
    val fileId: Int
) : KlibFileMetadata()
