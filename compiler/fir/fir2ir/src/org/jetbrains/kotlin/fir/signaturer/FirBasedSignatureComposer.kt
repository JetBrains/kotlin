/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.signaturer

import org.jetbrains.kotlin.KtFakeSourceElementKind
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.Fir2IrSignatureComposer
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
class FirBasedSignatureComposer(override val mangler: FirMangler) : Fir2IrSignatureComposer {
    private var fileSignature: IdSignature.FileSignature? = null

    override fun withFileSignature(sig: IdSignature.FileSignature, body: () -> Unit) {
        fileSignature = sig
        body()
        fileSignature = null
    }

    private data class FirDeclarationWithParentId(val declaration: FirDeclaration, val classId: ClassId?, val forceExpect: Boolean)

    private val signatureCache = mutableMapOf<FirDeclarationWithParentId, IdSignature.CommonSignature>()

    inner class SignatureBuilder(private val forceExpect: Boolean) : FirVisitor<Unit, Any?>() {
        var hashId: Long? = null
        var mask = 0L

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

        override fun visitTypeAlias(typeAlias: FirTypeAlias, data: Any?) {
            setExpected(typeAlias.isExpect)
        }

        override fun visitConstructor(constructor: FirConstructor, data: Any?) {
            hashId = mangler.run { constructor.signatureMangle(compatibleMode = false) }
            setExpected(constructor.isExpect)
        }

        override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: Any?) {
            hashId = mangler.run { simpleFunction.signatureMangle(compatibleMode = false) }
            setExpected(simpleFunction.isExpect)
        }

        override fun visitProperty(property: FirProperty, data: Any?) {
            hashId = mangler.run { property.signatureMangle(compatibleMode = false) }
            setExpected(property.isExpect)
        }

        override fun visitField(field: FirField, data: Any?) {
            hashId = mangler.run { field.signatureMangle(compatibleMode = false) }
            setExpected(field.isExpect)
        }

        override fun visitEnumEntry(enumEntry: FirEnumEntry, data: Any?) {
            setExpected(enumEntry.isExpect)
        }
    }

    override fun composeSignature(
        declaration: FirDeclaration,
        containingClass: ConeClassLikeLookupTag?,
        forceTopLevelPrivate: Boolean,
        forceExpect: Boolean,
    ): IdSignature? {
        if (declaration is FirAnonymousObject || declaration is FirAnonymousFunction) return null
        if (declaration is FirRegularClass && declaration.classId.isLocal) return null
        if (declaration is FirCallableDeclaration) {
            if (declaration.visibility == Visibilities.Local) return null
            if (declaration is FirField && declaration.source?.kind == KtFakeSourceElementKind.ClassDelegationField) return null
            if (declaration.dispatchReceiverClassLookupTagOrNull()?.classId?.isLocal == true || containingClass?.classId?.isLocal == true) return null
        }

        val declarationWithParentId = FirDeclarationWithParentId(declaration, containingClass?.classId, forceExpect)
        val publicSignature = signatureCache.getOrPut(declarationWithParentId) {
            calculatePublicSignature(declarationWithParentId)
        }

        val resultSignature: IdSignature = if (isTopLevelPrivate(declaration) || forceTopLevelPrivate) {
            val fileSig = fileSignature ?: declaration.fakeFileSignature(publicSignature)
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
                    classId.packageFqName.asString(), classId.relativeClassName.asString(), builder.hashId, builder.mask
                )
            }
            is FirTypeAlias -> {
                val classId = declaration.symbol.classId
                IdSignature.CommonSignature(
                    classId.packageFqName.asString(), classId.relativeClassName.asString(), builder.hashId, builder.mask
                )
            }
            is FirCallableDeclaration -> {
                val classId = containingClassId ?: declaration.containingClassLookupTag()?.classId
                val packageName = classId?.packageFqName ?: declaration.symbol.callableId.packageName
                val callableName = declaration.irName

                IdSignature.CommonSignature(
                    packageName.asString(),
                    classId?.relativeClassName?.child(callableName)?.asString() ?: callableName.asString(),
                    builder.hashId, builder.mask
                )
            }
            is FirScript -> {
                IdSignature.CommonSignature(
                    declaration.name.asString(), // TODO: find package id
                    declaration.name.asString(),
                    builder.hashId, builder.mask
                )
            }
            else -> error("Unsupported FIR declaration in signature composer: ${declaration.render()}")
        }
    }

    override fun composeTypeParameterSignature(
        typeParameter: FirTypeParameter,
        index: Int,
        containerSignature: IdSignature?
    ): IdSignature? {
        if (containerSignature == null) return null
        return IdSignature.CompositeSignature(
            containerSignature,
            IdSignature.LocalSignature(MangleConstant.TYPE_PARAMETER_MARKER_NAME, index.toLong(), null)
        )
    }

    override fun composeAccessorSignature(
        property: FirProperty,
        isSetter: Boolean,
        containingClass: ConeClassLikeLookupTag?,
        forceTopLevelPrivate: Boolean
    ): IdSignature? {
        val propSig: IdSignature.CommonSignature
        val fileSig: IdSignature.FileSignature?
        when (val propertySignature = composeSignature(property, containingClass, forceTopLevelPrivate)) {
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
        val id = with(mangler) {
            if (isSetter) {
                property.setterOrDefault().signatureMangle(compatibleMode = false)
            } else {
                property.getterOrDefault().signatureMangle(compatibleMode = false)
            }
        }
        val accessorFqName = if (isSetter) {
            propSig.declarationFqName + ".<set-${property.name.asString()}>"
        } else {
            propSig.declarationFqName + ".<get-${property.name.asString()}>"
        }
        val commonSig = IdSignature.CommonSignature(propSig.packageFqName, accessorFqName, id, propSig.mask)
        val accessorSig = IdSignature.AccessorSignature(propSig, commonSig)
        return if (fileSig != null) {
            IdSignature.CompositeSignature(fileSig, accessorSig)
        } else accessorSig
    }

    private fun isTopLevelPrivate(declaration: FirDeclaration): Boolean =
        declaration.symbol.getOwnerLookupTag() == null && declaration is FirMemberDeclaration && declaration.visibility == Visibilities.Private

    // We only need file signatures to distinguish between declarations with the same fqName across different files,
    // so FirDeclaration itself is an appropriate id.
    private fun FirDeclaration.fakeFileSignature(commonSignature: IdSignature.CommonSignature) =
        IdSignature.FileSignature(
            this, FqName(commonSignature.packageFqName + "." + commonSignature.declarationFqName), "<unknown>"
        )

    private fun FirProperty.getterOrDefault() =
        getter ?: FirDefaultPropertyGetter(
            source = null,
            moduleData, origin, returnTypeRef, visibility, symbol
        )

    private fun FirProperty.setterOrDefault() =
        setter ?: FirDefaultPropertySetter(
            source = null,
            moduleData, origin, returnTypeRef, visibility, symbol
        )

}
