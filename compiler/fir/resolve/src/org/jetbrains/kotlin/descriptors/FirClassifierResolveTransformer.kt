/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors

import org.jetbrains.kotlin.fir.FirDescriptorOwner
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedClassImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedEnumEntryImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedTypeAliasImpl
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedTypeParameterImpl
import org.jetbrains.kotlin.fir.descriptors.ConeClassifierDescriptor
import org.jetbrains.kotlin.fir.descriptors.ConeTypeParameterDescriptor
import org.jetbrains.kotlin.fir.types.FirResolvedType
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.FirTransformer
import org.jetbrains.kotlin.fir.visitors.compose
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class FirClassifierResolveTransformer : FirTransformer<Nothing?>() {
    override fun <E : FirElement> transformElement(element: E, data: Nothing?): CompositeTransformResult<E> {
        return element.compose()
    }

    lateinit var packageFqName: FqName

    override fun transformFile(file: FirFile, data: Nothing?): CompositeTransformResult<FirFile> {
        packageFqName = file.packageFqName
        return file.also { it.transformChildren(this, null) }.compose()
    }

    private var classLikeName: FqName = FqName.ROOT

    override fun transformClass(klass: FirClass, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        val actualClassName = classLikeName.child(klass.name)
        classLikeName = actualClassName

        klass.transformChildren(this, data)

        val superTypes = klass.superTypes.map {
            (it as FirResolvedType).type
        }

        classLikeName = classLikeName.parent()

        val typeParameters =
            klass.typeParameters.filterIsInstance<FirDescriptorOwner<*>>().mapNotNull { it.descriptor as? ConeTypeParameterDescriptor }

        val nestedClassifiers =
            klass.declarations.filterIsInstance<FirDescriptorOwner<*>>().mapNotNull { it.descriptor as? ConeClassifierDescriptor }

        val classId = ClassId(packageFqName, actualClassName, false)

        return when (klass) {
            is FirEnumEntry -> {
                val descriptor = ConeEnumEntryDescriptorImpl(typeParameters, classId, superTypes, nestedClassifiers)
                FirResolvedEnumEntryImpl(klass, descriptor).compose()
            }
            else -> {
                val descriptor = ConeClassDescriptorImpl(typeParameters, classId, superTypes, nestedClassifiers)
                FirResolvedClassImpl(klass, descriptor).compose()
            }
        }
    }

    override fun transformResolvedClass(resolvedClass: FirResolvedClass, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        return resolvedClass.compose()
    }

    var memberName: Name? = null

    override fun transformNamedFunction(namedFunction: FirNamedFunction, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        memberName = namedFunction.name
        return super.transformNamedFunction(namedFunction, data).also {
            memberName = null
        }
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        val actualClassName = classLikeName.child(typeAlias.name)
        classLikeName = actualClassName

        typeAlias.transformChildren(this, data)

        classLikeName = classLikeName.parent()

        val expandedType = (typeAlias.expandedType as FirResolvedType).type
        val typeParameters =
            typeAlias.typeParameters.filterIsInstance<FirDescriptorOwner<*>>().mapNotNull { it.descriptor as? ConeTypeParameterDescriptor }

        val descriptor = ConeTypeAliasDescriptorImpl(
            typeParameters,
            ClassId(packageFqName, classLikeName.child(typeAlias.name), false),
            expandedType
        )

        return FirResolvedTypeAliasImpl(typeAlias, descriptor).compose()
    }

    override fun transformResolvedTypeAlias(
        resolvedTypeAlias: FirResolvedTypeAlias,
        data: Nothing?
    ): CompositeTransformResult<FirDeclaration> {
        return resolvedTypeAlias.compose()
    }

    override fun transformResolvedTypeParameter(
        resolvedTypeParameter: FirResolvedTypeParameter,
        data: Nothing?
    ): CompositeTransformResult<FirDeclaration> {
        return resolvedTypeParameter.compose()
    }

    override fun transformTypeParameter(typeParameter: FirTypeParameter, data: Nothing?): CompositeTransformResult<FirDeclaration> {
        typeParameter.transformChildren(this, data)

        val descriptor = ConeTypeParameterDescriptorImpl(typeParameter.symbol)
        return FirResolvedTypeParameterImpl(typeParameter, descriptor).compose()
    }
}