/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.functions.FunctionalTypeKind
import org.jetbrains.kotlin.builtins.functions.FunctionalTypeKindExtractor
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirFunctionalTypeKindExtension
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.functionalTypeKindExtensions

class FirFunctionalTypeKindServiceImpl(session: FirSession) : FirFunctionalTypeKindService() {
    override val extractor: FunctionalTypeKindExtractor = run {
        val kinds = buildList {
            add(FunctionalTypeKind.Function)
            add(FunctionalTypeKind.SuspendFunction)
            add(FunctionalTypeKind.KFunction)
            add(FunctionalTypeKind.KSuspendFunction)

            val registrar = object : FirFunctionalTypeKindExtension.FunctionalTypeKindRegistrar {
                override fun registerKind(nonReflectKind: FunctionalTypeKind, reflectKind: FunctionalTypeKind) {
                    require(nonReflectKind.reflectKind() == reflectKind)
                    require(reflectKind.nonReflectKind() == nonReflectKind)
                    add(nonReflectKind)
                    add(reflectKind)
                }
            }

            for (extension in session.extensionService.functionalTypeKindExtensions) {
                with(extension) { registrar.registerKinds() }
            }
        }.also { kinds ->
            val allNames = kinds.map { "${it.packageFqName}.${it.classNamePrefix}" }
            require(allNames.distinct() == allNames) {
                "There are clashing functional type kinds: $allNames"
            }
        }
        FunctionalTypeKindExtractor(kinds)
    }
}
