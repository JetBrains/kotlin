/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.signaturer

import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.FirMangler
import org.jetbrains.kotlin.fir.backend.conversionData
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertyGetter
import org.jetbrains.kotlin.fir.declarations.impl.FirDefaultPropertySetter
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName

// @NoMutableState -- we'll restore this annotation once we get rid of withFileSignature().
class FirBasedSignatureComposer(val mangler: FirMangler) {
    private data class FirDeclarationWithParentId(val declaration: FirDeclaration, val classId: ClassId?, val forceExpect: Boolean)

    private val signatureCache = mutableMapOf<FirDeclarationWithParentId, IdSignature.CommonSignature>()

    private fun computeSignatureHashAndDescriptionFor(declaration: FirDeclaration): Pair<Long, String> = mangler.run {
        declaration.signatureString(compatibleMode = false).let { it.hashMangle to it }
    }

    fun composeSignature(declaration: FirClassLikeDeclaration): IdSignature? {
        return composeSignatureImpl(declaration, containingClass = null, forceExpect = false)
    }

    fun composeSignature(
        declaration: FirCallableDeclaration,
        containingClass: ConeClassLikeLookupTag? = null,
        forceExpect: Boolean = false
    ): IdSignature? {
        return composeSignatureImpl(declaration, containingClass, forceExpect)
    }

    fun composeSignature(declaration: FirScript): IdSignature? {
        return composeSignatureImpl(declaration, containingClass = null, forceExpect = false)
    }

    fun composeSignature(declaration: FirCodeFragment): IdSignature? {
        return composeSignatureImpl(declaration, containingClass = null, forceExpect = false)
    }

    fun composeTypeParameterSignature(
        index: Int,
        containerSignature: IdSignature?
    ): IdSignature? {
        if (containerSignature == null) return null
        return IdSignature.CompositeSignature(
            containerSignature,
            IdSignature.LocalSignature(MangleConstant.TYPE_PARAMETER_MARKER_NAME, index.toLong(), null)
        )
    }

    fun composeAccessorSignature(
        property: FirProperty,
        isSetter: Boolean,
        containingClass: ConeClassLikeLookupTag? = null
    ): IdSignature? {
        val propSig: IdSignature.CommonSignature
        val fileSig: IdSignature.FileSignature?
        when (val propertySignature = composeSignature(property, containingClass)) {
            is IdSignature.CompositeSignature -> {
                propSig = propertySignature.inner as? IdSignature.CommonSignature ?: return null
                fileSig = propertySignature.container as? IdSignature.FileSignature ?: return null
            }
            is IdSignature.CommonSignature -> {
                propSig = propertySignature
                fileSig = null
            }
            else -> return null
        }
        val accessor = if (isSetter) {
            property.setterOrDefault()
        } else {
            property.getterOrDefault()
        }
        val (id, description) = computeSignatureHashAndDescriptionFor(accessor)
        val accessorFqName = "${propSig.declarationFqName}.${accessor.irName}"
        val commonSig = IdSignature.CommonSignature(
            packageFqName = propSig.packageFqName,
            declarationFqName = accessorFqName,
            id = id,
            mask = propSig.mask,
            description = description,
        )
        val accessorSig = IdSignature.AccessorSignature(propSig, commonSig)
        return if (fileSig != null) {
            IdSignature.CompositeSignature(fileSig, accessorSig)
        } else accessorSig
    }

    private fun composeSignatureImpl(
        declaration: FirDeclaration,
        containingClass: ConeClassLikeLookupTag?,
        forceExpect: Boolean
    ): IdSignature? {
        if (declaration is FirAnonymousObject || declaration is FirAnonymousFunction) return null
        if (declaration is FirRegularClass && declaration.classId.isLocal) return null
        if (declaration is FirCallableDeclaration) {
            if (declaration.visibility == Visibilities.Local) return null
            if (declaration.dispatchReceiverClassLookupTagOrNull()?.classId?.isLocal == true || containingClass?.classId?.isLocal == true) return null
        }

        val declarationWithParentId = FirDeclarationWithParentId(declaration, containingClass?.classId, forceExpect)
        val publicSignature = signatureCache.getOrPut(declarationWithParentId) {
            calculatePublicSignature(declarationWithParentId)
        }

        val resultSignature: IdSignature = if (isTopLevelPrivate(declaration)) {
            val fileSig = declaration.fakeFileSignature(publicSignature)
            IdSignature.CompositeSignature(fileSig, publicSignature)
        } else
            publicSignature

        return resultSignature
    }

    private fun calculatePublicSignature(declarationWithParentId: FirDeclarationWithParentId): IdSignature.CommonSignature {
        val (declaration, containingClassId) = declarationWithParentId
        val builder = SignatureBuilder(declarationWithParentId.forceExpect)
        try {
            declaration.accept(builder, null)
        } catch (t: Throwable) {
            throw IllegalStateException("Error while composing signature for ${declaration.render()}", t)
        }
        return when (declaration) {
            is FirRegularClass -> {
                // TODO: private classes are probably not acceptable here too
                val classId = declaration.classId
                IdSignature.CommonSignature(
                    packageFqName = classId.packageFqName.asString(),
                    declarationFqName = classId.relativeClassName.asString(),
                    id = builder.hashId,
                    mask = builder.mask,
                    description = builder.description,
                )
            }
            is FirTypeAlias -> {
                val classId = declaration.symbol.classId
                IdSignature.CommonSignature(
                    packageFqName = classId.packageFqName.asString(),
                    declarationFqName = classId.relativeClassName.asString(),
                    id = builder.hashId,
                    mask = builder.mask,
                    description = builder.description,
                )
            }
            is FirCallableDeclaration -> {
                val classId = containingClassId ?: declaration.containingClassLookupTag()?.classId
                val packageName = classId?.packageFqName ?: declaration.symbol.callableId.packageName
                val callableName = declaration.irName

                IdSignature.CommonSignature(
                    packageFqName = packageName.asString(),
                    declarationFqName = classId?.relativeClassName?.child(callableName)?.asString() ?: callableName.asString(),
                    id = builder.hashId,
                    mask = builder.mask,
                    description = builder.description,
                )
            }
            is FirScript -> {
                IdSignature.CommonSignature(
                    packageFqName = declaration.name.asString(),
                    declarationFqName = declaration.name.asString(),
                    id = builder.hashId,
                    mask = builder.mask,
                    description = builder.description,
                )
            }
            is FirCodeFragment -> {
                val conversionData = declaration.conversionData
                val packageFqName = conversionData.classId.packageFqName.asString()
                val classFqName = conversionData.classId.relativeClassName.asString()
                IdSignature.CommonSignature(packageFqName, classFqName, builder.hashId, builder.mask, description = null)
            }
            else -> error("Unsupported FIR declaration in signature composer: ${declaration.render()}")
        }
    }

    private inner class SignatureBuilder(private val forceExpect: Boolean) : FirVisitor<Unit, Any?>() {
        var hashId: Long? = null
        var description: String? = null
        var mask = 0L

        private fun setHashAndDescriptionFor(declaration: FirDeclaration) {
            computeSignatureHashAndDescriptionFor(declaration).let {
                hashId = it.first
                description = it.second
            }
        }

        private fun setExpected(f: Boolean) {
            mask = mask or IdSignature.Flags.IS_EXPECT.encode(f || forceExpect)
        }

        override fun visitElement(element: FirElement, data: Any?) {
            TODO("Should not be here")
        }

        override fun visitRegularClass(regularClass: FirRegularClass, data: Any?) {
            setExpected(regularClass.isExpect)
            //platformSpecificClass(descriptor)
        }

        override fun visitScript(script: FirScript, data: Any?) {
        }

        override fun visitCodeFragment(codeFragment: FirCodeFragment, data: Any?) {
        }

        override fun visitTypeAlias(typeAlias: FirTypeAlias, data: Any?) {
            setExpected(typeAlias.isExpect)
        }

        override fun visitConstructor(constructor: FirConstructor, data: Any?) {
            setHashAndDescriptionFor(constructor)
            setExpected(constructor.isExpect)
        }

        override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: Any?) {
            setHashAndDescriptionFor(simpleFunction)
            setExpected(simpleFunction.isExpect)
        }

        override fun visitProperty(property: FirProperty, data: Any?) {
            setHashAndDescriptionFor(property)
            setExpected(property.isExpect)
        }

        override fun visitField(field: FirField, data: Any?) {
            setHashAndDescriptionFor(field)
            setExpected(field.isExpect)
        }

        override fun visitEnumEntry(enumEntry: FirEnumEntry, data: Any?) {
            setExpected(enumEntry.isExpect)
        }
    }

    private fun isTopLevelPrivate(declaration: FirDeclaration): Boolean =
        declaration.symbol.getOwnerLookupTag() == null && declaration is FirMemberDeclaration && declaration.visibility == Visibilities.Private

    // We only need file signatures to distinguish between declarations with the same fqName across different files,
    // so FirDeclaration itself is an appropriate id.
    private fun FirDeclaration.fakeFileSignature(commonSignature: IdSignature.CommonSignature): IdSignature.FileSignature {
        return IdSignature.FileSignature(
            this, FqName(commonSignature.packageFqName + "." + commonSignature.declarationFqName), "<unknown>"
        )
    }

    private fun FirProperty.getterOrDefault(): FirPropertyAccessor {
        return getter ?: FirDefaultPropertyGetter(
            source = null,
            moduleData, origin, returnTypeRef, visibility, symbol
        )
    }

    private fun FirProperty.setterOrDefault(): FirPropertyAccessor {
        return setter ?: FirDefaultPropertySetter(
            source = null,
            moduleData, origin, returnTypeRef, visibility, symbol
        )
    }

}
