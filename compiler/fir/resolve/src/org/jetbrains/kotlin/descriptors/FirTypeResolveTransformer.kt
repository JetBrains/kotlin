/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.resolve.FirTypeResolver
import org.jetbrains.kotlin.fir.scopes.FirImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirCompositeImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirExplicitImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirSelfImportingScope
import org.jetbrains.kotlin.fir.types.FirResolvedType
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeImpl
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose

class FirTypeResolveTransformer : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return (element.transformChildren(this, data) as E).compose()
    }

    lateinit var importingScope: FirImportingScope

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        importingScope = FirCompositeImportingScope(
            FirExplicitImportingScope(file.imports),
            FirSelfImportingScope(file.packageFqName, file.session)
        )
        return file.also { it.transformChildren(this, null) }.compose()
    }

    override fun transformType(type: FirType, data: Nothing?): CompositeTransformResult<FirType> {
        val typeResolver = FirTypeResolver.getInstance(type.session)
        return FirResolvedTypeImpl(
            type.session,
            type.psi,
            typeResolver.resolveType(type, importingScope),
            false,
            type.annotations
        ).compose()
    }

    override fun transformResolvedType(resolvedType: FirResolvedType, data: Nothing?): CompositeTransformResult<FirType> {
        return resolvedType.compose()
    }
}