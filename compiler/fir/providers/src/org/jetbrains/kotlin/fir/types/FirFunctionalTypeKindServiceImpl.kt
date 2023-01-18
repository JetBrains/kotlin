/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.types

import org.jetbrains.kotlin.builtins.functions.FunctionClassKind
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.extensions.FirFunctionalTypeKindExtension
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.functionalTypeKindExtensions
import org.jetbrains.kotlin.name.FqName

class FirFunctionalTypeKindServiceImpl(session: FirSession) : FirFunctionalTypeKindService() {
    private val knownKindsByPackageFqName = buildList {
        add(ConeFunctionalTypeKind.Function)
        add(ConeFunctionalTypeKind.SuspendFunction)
        add(ConeFunctionalTypeKind.KFunction)
        add(ConeFunctionalTypeKind.KSuspendFunction)

        val registrar = object : FirFunctionalTypeKindExtension.FunctionalTypeKindRegistrar {
            override fun registerKind(nonReflectKind: ConeFunctionalTypeKind, reflectKind: ConeFunctionalTypeKind) {
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
    }.groupBy { it.packageFqName }

    override fun getKindByClassNamePrefix(packageFqName: FqName, className: String): ConeFunctionalTypeKind? {
        val kinds = knownKindsByPackageFqName[packageFqName] ?: return null
        for (kind in kinds) {
            if (!className.startsWith(kind.classNamePrefix)) continue
            if (!FunctionClassKind.hasArityAtTheEnd(kind.classNamePrefix, className)) continue
            return kind
        }
        return null
    }
}
