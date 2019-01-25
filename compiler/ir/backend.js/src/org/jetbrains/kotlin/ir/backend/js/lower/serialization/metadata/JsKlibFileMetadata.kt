/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower.serialization.metadata

import org.jetbrains.kotlin.psi.KtFile

sealed class JsKlibFileMetadata

data class KotlinPsiFileMetadata(val ktFile: KtFile) : JsKlibFileMetadata()

data class KotlinDeserializedFileMetadata(
    val packageFragment: JsKlibMetadataPackageFragment,
    val fileId: Int
) : JsKlibFileMetadata()
