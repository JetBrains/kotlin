/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.visitors.FirTransformer

class FirTotalResolveTransformer {

    val transformers: List<FirTransformer<Nothing?>> = listOf(
        FirImportResolveTransformer(),
        FirSupertypeResolverTransformer(),
        FirTypeResolveTransformer(),
        FirStatusResolveTransformer(),
        FirAccessResolveTransformer()
    )

    fun processFiles(files: List<FirFile>) {
        for (transformer in transformers) {
            for (firFile in files) {
                firFile.transform<FirFile, Nothing?>(transformer, null)
            }
        }
    }
}
