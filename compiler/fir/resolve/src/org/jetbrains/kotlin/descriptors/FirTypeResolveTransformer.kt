/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirClass
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.declarations.FirTypeAlias
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.FirTypeResolver
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.impl.FirCompositeScope
import org.jetbrains.kotlin.fir.scopes.impl.FirExplicitImportingScope
import org.jetbrains.kotlin.fir.scopes.impl.FirNestedClassifierScope
import org.jetbrains.kotlin.fir.scopes.impl.FirSelfImportingScope
import org.jetbrains.kotlin.fir.transformInplace
import org.jetbrains.kotlin.fir.types.ConeClassType
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.FirResolvedType
import org.jetbrains.kotlin.fir.types.FirType
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeImpl
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

class FirTypeResolveTransformer : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return (element.transformChildren(this, data) as E).compose()
    }

    lateinit var scope: FirCompositeScope
    lateinit var packageFqName: FqName
    var className: FqName = FqName.ROOT

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        scope = FirCompositeScope(
            mutableListOf(
                FirExplicitImportingScope(file.imports),
                FirSelfImportingScope(file.packageFqName, file.session)
            )
        )
        packageFqName = file.packageFqName
        return file.also { it.transformChildren(this, null) }.compose()
    }

    override fun transformClass(klass: FirClass, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        klass.transformChildren(SuperTypeResolver(), null)
        className = className.child(klass.name)
        scope.scopes += FirNestedClassifierScope(ClassId(packageFqName, className, false), klass.session)
        val result = super.transformClass(klass, data)
        scope.scopes.also { it.removeAt(it.lastIndex) }
        className = className.parent()

        return result
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        typeAlias.transformChildren(SuperTypeResolver(), null)
        return super.transformTypeAlias(typeAlias, data)
    }

    override fun transformType(type: FirType, data: Nothing?): CompositeTransformResult<FirType> {
        val typeResolver = FirTypeResolver.getInstance(type.session)
        return FirResolvedTypeImpl(
            type.session,
            type.psi,
            typeResolver.resolveType(type, scope),
            false,
            type.annotations
        ).compose()
    }

    override fun transformResolvedType(resolvedType: FirResolvedType, data: Nothing?): CompositeTransformResult<FirType> {
        return resolvedType.compose()
    }


    private inner class SuperTypeResolver : FirTransformer<Nothing?>() {
        override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
            return element.compose()
        }

        override fun transformType(type: FirType, data: Nothing?): CompositeTransformResult<FirType> {
            val transformedType = this@FirTypeResolveTransformer.transformType(type, data).single as FirResolvedType

            val classId = (transformedType.type as? ConeClassType)?.fqName ?: return transformedType.compose()
            val firProvider = FirProvider.getInstance(transformedType.session)
            // ???
            firProvider.getFirClassifierByFqName(classId)!!.transformChildren(this, data)

            return transformedType.compose()
        }
    }
}