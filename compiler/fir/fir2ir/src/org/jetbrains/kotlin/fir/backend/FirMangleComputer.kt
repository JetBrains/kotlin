/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend

import org.jetbrains.kotlin.backend.common.serialization.mangle.BaseKotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.collectForMangler
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.signaturer.irName
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.ConeTypeParameterLookupTag
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid

/**
 * A base mangle computer that generates a mangled name for a Kotlin declaration represented by [FirDeclaration].
 */
open class FirMangleComputer(
    builder: StringBuilder,
    mode: MangleMode,
) : BaseKotlinMangleComputer<
        /*Declaration=*/FirDeclaration,
        /*Type=*/ConeKotlinType,
        /*TypeParameter=*/ConeTypeParameterLookupTag,
        /*ValueParameter=*/FirValueParameter,
        /*TypeParameterContainer=*/FirMemberDeclaration,
        /*FunctionDeclaration=*/FirFunction,
        /*Session=*/FirSession,
        >(builder, mode) {
    protected open val visitor = Visitor()

    override fun copy(newMode: MangleMode): FirMangleComputer =
        FirMangleComputer(builder, newMode)

    override fun getTypeSystemContext(session: FirSession) = object : ConeInferenceContext {
        override val session: FirSession
            get() = session
    }

    @OptIn(SymbolInternals::class)
    override fun FirDeclaration.visitParent() {
        val (parentPackageFqName, parentClassId) = when (this) {
            is FirCallableDeclaration -> this.symbol.callableId.packageName.let { it to containingClassLookupTag()?.classId }
            is FirClassLikeDeclaration -> this.symbol.classId.let { it.packageFqName to it.outerClassId }
            else -> return
        }
        if (parentClassId != null && !parentClassId.isLocal) {
            val parentClassLike = this.moduleData.session.symbolProvider.getClassLikeSymbolByClassId(parentClassId)?.fir
                ?: error("Attempt to find parent ($parentClassId) for probably-local declaration!")
            if (parentClassLike is FirRegularClass || parentClassLike is FirTypeAlias) {
                parentClassLike.visit()
            } else {
                error("Strange class-like declaration: ${parentClassLike.render()}")
            }
        } else if (parentClassId == null && !parentPackageFqName.isRoot) {
            builder.appendName(parentPackageFqName.asString())
        }
    }

    override fun FirDeclaration.visit() {
        accept(visitor, null)
    }

    private fun FirFunction.mangleFunction(isCtor: Boolean, isStatic: Boolean, container: FirDeclaration) {

        isRealExpect = isRealExpect || (this as? FirMemberDeclaration)?.isExpect == true

        if (container is FirMemberDeclaration) {
            typeParameterContainers.add(container)
        }
        visitParent()

        builder.appendName(MangleConstant.FUNCTION_NAME_PREFIX)

        platformSpecificFunctionName()?.let {
            builder.append(it)
            return
        }

        val name = this.irName
        builder.append(name.asString())

        platformSpecificSuffix()?.let {
            builder.append(MangleConstant.PLATFORM_FUNCTION_MARKER)
            builder.append(it)
        }

        mangleSignature(isCtor, isStatic, container)
    }

    private fun FirFunction.mangleSignature(isCtor: Boolean, isStatic: Boolean, container: FirDeclaration) {
        if (!mode.signature) {
            return
        }

        if (isStatic) {
            builder.appendSignature(MangleConstant.STATIC_MEMBER_MARK)
        }

        contextReceivers.forEach {
            builder.appendSignature(MangleConstant.CONTEXT_RECEIVER_PREFIX)
            mangleType(builder, it.typeRef.coneType, moduleData.session)
        }

        val receiverType = receiverParameter?.typeRef ?: (this as? FirPropertyAccessor)?.propertySymbol?.fir?.receiverParameter?.typeRef
        receiverType?.let {
            builder.appendSignature(MangleConstant.EXTENSION_RECEIVER_PREFIX)
            mangleType(builder, it.coneType, moduleData.session)
        }

        valueParameters.collectForMangler(builder, MangleConstant.VALUE_PARAMETERS) {
            appendSignature(specialValueParamPrefix(it))
            mangleValueParameter(this, it, moduleData.session)
        }
        (container as? FirTypeParametersOwner)?.typeParameters?.withIndex()?.toList().orEmpty()
            .collectForMangler(builder, MangleConstant.TYPE_PARAMETERS) { (index, typeParameter) ->
                mangleTypeParameter(this, typeParameter.symbol.toLookupTag(), index, moduleData.session)
            }

        if (!isCtor && !returnTypeRef.isUnit && addReturnType()) {
            mangleType(builder, returnTypeRef.coneType, moduleData.session)
        }
    }

    @OptIn(SymbolInternals::class)
    override fun getEffectiveParent(typeParameter: ConeTypeParameterLookupTag): FirMemberDeclaration = typeParameter.symbol.fir.run {
        this.containingDeclarationSymbol.fir as FirMemberDeclaration
    }

    override fun renderDeclaration(declaration: FirDeclaration) = declaration.render()

    override fun getTypeParameterName(typeParameter: ConeTypeParameterLookupTag) = typeParameter.name.asString()

    override fun isVararg(valueParameter: FirValueParameter) = valueParameter.isVararg

    override fun getValueParameterType(valueParameter: FirValueParameter) = valueParameter.returnTypeRef.coneType

    override fun getIndexOfTypeParameter(typeParameter: ConeTypeParameterLookupTag, container: FirMemberDeclaration) =
        container.typeParameters.indexOf(typeParameter.symbol.fir)

    override fun mangleType(tBuilder: StringBuilder, type: ConeKotlinType, declarationSiteSession: FirSession) {
        when (type) {
            is ConeLookupTagBasedType -> {
                when (val symbol = type.lookupTag.toSymbol(declarationSiteSession)) {
                    is FirTypeAliasSymbol -> {
                        mangleType(tBuilder, type.fullyExpandedType(declarationSiteSession), declarationSiteSession)
                        return
                    }

                    is FirClassSymbol -> with(copy(MangleMode.FQNAME)) { symbol.fir.visit() }
                    is FirTypeParameterSymbol -> tBuilder.mangleTypeParameterReference(symbol.toLookupTag())
                    // This is performed for a case with invisible class-like symbol in fake override
                    null -> (type.lookupTag as? ConeClassLikeLookupTag)?.let {
                        tBuilder.append(it.classId)
                    }
                }

                mangleTypeArguments(tBuilder, type, declarationSiteSession)

                if (type.isMarkedNullable) {
                    tBuilder.appendSignature(MangleConstant.Q_MARK)
                }

                if (type.hasEnhancedNullability) {
                    tBuilder.appendSignature(MangleConstant.ENHANCED_NULLABILITY_MARK)
                }
            }

            is ConeRawType -> {
                mangleType(tBuilder, type.lowerBound, declarationSiteSession)
            }

            is ConeFlexibleType -> {
                with(declarationSiteSession.typeContext) {
                    // Need to reproduce type approximation done for flexible types in TypeTranslator.
                    // For now, we replicate the current behaviour of Fir2IrTypeConverter and just take the upper bound
                    val upper = type.upperBound
                    if (upper is ConeClassLikeType) {
                        val lower = type.lowerBound as? ConeClassLikeType ?: error("Expecting class-like type, got ${type.lowerBound}")
                        val intermediate = if (lower.lookupTag == upper.lookupTag) {
                            lower.replaceArguments(upper.getArguments())
                        } else lower
                        val mixed = if (upper.isNullable) intermediate.makeNullable() else intermediate.makeDefinitelyNotNullOrNotNull()
                        mangleType(tBuilder, mixed as ConeKotlinType, declarationSiteSession)
                    } else mangleType(tBuilder, upper, declarationSiteSession)
                }
            }

            is ConeDefinitelyNotNullType -> {
                // E.g. not-null type parameter in Java
                mangleType(tBuilder, type.original, declarationSiteSession)
            }

            is ConeCapturedType -> {
                mangleType(tBuilder, type.lowerType ?: type.constructor.supertypes!!.first(), declarationSiteSession)
            }

            is ConeIntersectionType -> {
                // TODO: add intersectionTypeApproximation
                mangleType(tBuilder, type.intersectedTypes.first(), declarationSiteSession)
            }

            else -> error("Unexpected type $type")
        }
    }

    protected open inner class Visitor : FirVisitorVoid() {

        override fun visitElement(element: FirElement) = error("unexpected element ${element.render()}")

        override fun visitRegularClass(regularClass: FirRegularClass) {
            isRealExpect = isRealExpect or regularClass.isExpect
            typeParameterContainers.add(regularClass)
            regularClass.mangleSimpleDeclaration(regularClass.name.asString())
        }

        override fun visitAnonymousObject(anonymousObject: FirAnonymousObject) {
            anonymousObject.mangleSimpleDeclaration("<anonymous>")
        }

        override fun visitVariable(variable: FirVariable) {
            isRealExpect = isRealExpect or variable.isExpect
            typeParameterContainers.add(variable)
            variable.visitParent()

            val isStaticProperty = variable.isStatic
            if (isStaticProperty) {
                builder.appendSignature(MangleConstant.STATIC_MEMBER_MARK)
            }

            variable.receiverParameter?.typeRef?.let {
                builder.appendSignature(MangleConstant.EXTENSION_RECEIVER_PREFIX)
                mangleType(builder, it.coneType, variable.moduleData.session)
            }

            variable.typeParameters.withIndex().toList()
                .collectForMangler(builder, MangleConstant.TYPE_PARAMETERS) { (index, typeParameter) ->
                    mangleTypeParameter(this, typeParameter.symbol.toLookupTag(), index, variable.moduleData.session)
                }

            builder.append(variable.name.asString())
        }

        override fun visitProperty(property: FirProperty) {
            visitVariable(property)
        }

        override fun visitField(field: FirField) {
            visitVariable(field)
        }

        override fun visitEnumEntry(enumEntry: FirEnumEntry) {
            enumEntry.mangleSimpleDeclaration(enumEntry.name.asString())
        }

        override fun visitTypeAlias(typeAlias: FirTypeAlias) =
            typeAlias.mangleSimpleDeclaration(typeAlias.name.asString())

        override fun visitSimpleFunction(simpleFunction: FirSimpleFunction) {
            isRealExpect = isRealExpect || simpleFunction.isExpect
            val isStatic = simpleFunction.isStatic
            simpleFunction.mangleFunction(false, isStatic, simpleFunction)
        }

        override fun visitConstructor(constructor: FirConstructor) =
            constructor.mangleFunction(isCtor = true, isStatic = false, constructor)

        override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor) {
            if (propertyAccessor is FirSyntheticPropertyAccessor) {
                // No need to distinguish between the accessor and its delegate.
                visitSimpleFunction(propertyAccessor.delegate)
            } else {
                propertyAccessor.mangleFunction(isCtor = false, propertyAccessor.isStatic, propertyAccessor.propertySymbol.fir)
            }
        }
    }
}