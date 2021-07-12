/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.signaturer

import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.backend.Fir2IrSignatureComposer
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.classId
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.visibility
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.ir.util.IdSignature

@NoMutableState
class FirBasedSignatureComposer(private val mangler: FirMangler) : Fir2IrSignatureComposer {
    inner class SignatureBuilder : FirVisitor<Unit, Any?>() {
        var hashId: Long? = null
        var mask = 0L

        private fun setExpected(f: Boolean) {
            mask = mask or IdSignature.Flags.IS_EXPECT.encode(f)
        }

        override fun visitElement(element: FirElement, data: Any?) {
            TODO("Should not be here")
        }

        override fun visitRegularClass(regularClass: FirRegularClass, data: Any?) {
            setExpected(regularClass.isExpect)
            //platformSpecificClass(descriptor)
        }

        override fun visitTypeAlias(typeAlias: FirTypeAlias, data: Any?) {
            setExpected(typeAlias.isExpect)
        }

        override fun visitConstructor(constructor: FirConstructor, data: Any?) {
            hashId = mangler.run { constructor.signatureMangle() }
            setExpected(constructor.isExpect)
        }

        override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: Any?) {
            hashId = mangler.run { simpleFunction.signatureMangle() }
            setExpected(simpleFunction.isExpect)
        }

        override fun visitProperty(property: FirProperty, data: Any?) {
            hashId = mangler.run { property.signatureMangle() }
            setExpected(property.isExpect)
        }

        override fun visitEnumEntry(enumEntry: FirEnumEntry, data: Any?) {
            setExpected(enumEntry.isExpect)
        }
    }

    override fun composeSignature(declaration: FirDeclaration, containingClass: ConeClassLikeLookupTag?): IdSignature? {
        if (declaration is FirAnonymousObject || declaration is FirAnonymousFunction) return null
        if (declaration is FirRegularClass && declaration.classId.isLocal) return null
        if (declaration is FirCallableDeclaration) {
            if (declaration.visibility == Visibilities.Local) return null
            if (declaration.dispatchReceiverClassOrNull()?.classId?.isLocal == true || containingClass?.classId?.isLocal == true) return null
        }
        val builder = SignatureBuilder()
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
                if (declaration.visibility == Visibilities.Private) return null
                val classId = declaration.symbol.classId
                IdSignature.CommonSignature(
                    classId.packageFqName.asString(), classId.relativeClassName.asString(), builder.hashId, builder.mask
                )
            }
            is FirCallableDeclaration -> {
                if (declaration.visibility == Visibilities.Private) return null
                val containingClassId = containingClass?.classId

                val classId = containingClassId ?: declaration.containingClass()?.classId
                val packageName = classId?.packageFqName ?: declaration.symbol.callableId.packageName
                val callableName = declaration.symbol.callableId.callableName

                IdSignature.CommonSignature(
                    packageName.asString(),
                    classId?.relativeClassName?.child(callableName)?.asString() ?: callableName.asString(),
                    builder.hashId, builder.mask
                )
            }
            else -> error("Unsupported FIR declaration in signature composer: ${declaration.render()}")
        }
    }

    override fun composeAccessorSignature(
        property: FirProperty,
        isSetter: Boolean,
        containingClass: ConeClassLikeLookupTag?
    ): IdSignature? {
        val propertySignature = composeSignature(property, containingClass) as? IdSignature.CommonSignature ?: return null
        val accessorFqName = if (isSetter) {
            propertySignature.declarationFqName + ".<set-${property.name.asString()}>"
        } else {
            propertySignature.declarationFqName + ".<get-${property.name.asString()}>"
        }
        return IdSignature.CommonSignature(propertySignature.packageFqName, accessorFqName, propertySignature.id, propertySignature.mask)
    }
}
