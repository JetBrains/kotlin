/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.collectForMangler
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.synthetic.FirSyntheticPropertyAccessor
import org.jetbrains.kotlin.fir.declarations.utils.isExpect
import org.jetbrains.kotlin.fir.declarations.utils.isStatic
import org.jetbrains.kotlin.fir.java.declarations.FirJavaField
import org.jetbrains.kotlin.fir.resolve.fullyExpandedType
import org.jetbrains.kotlin.fir.resolve.providers.firProvider
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.signaturer.irName
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeLookupTag
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeAliasSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.util.capitalizeDecapitalize.toLowerCaseAsciiOnly
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty

open class FirJvmMangleComputer(
    private val builder: StringBuilder,
    private val mode: MangleMode
) : FirVisitor<Unit, Boolean>(), KotlinMangleComputer<FirDeclaration> {

    private val typeParameterContainer = ArrayList<FirMemberDeclaration>(4)

    private var isRealExpect = false

    open fun FirFunction.platformSpecificFunctionName(): String? = null

    open fun FirFunction.platformSpecificSuffix(): String? =
        if (this is FirSimpleFunction && name.asString() == "main")
            this.moduleData.session.firProvider.getFirCallableContainerFile(symbol)?.name
        else null

    open fun FirFunction.specialValueParamPrefix(param: FirValueParameter): String = ""

    private fun addReturnType(): Boolean = true

    override fun copy(newMode: MangleMode): FirJvmMangleComputer =
        FirJvmMangleComputer(builder, newMode)

    private fun StringBuilder.appendName(s: String) {
        if (mode.fqn) {
            append(s)
        }
    }

    private fun StringBuilder.appendName(c: Char) {
        if (mode.fqn) {
            append(c)
        }
    }

    private fun StringBuilder.appendSignature(s: String) {
        if (mode.signature) {
            append(s)
        }
    }

    private fun StringBuilder.appendSignature(c: Char) {
        if (mode.signature) {
            append(c)
        }
    }

    private fun StringBuilder.appendSignature(i: Int) {
        if (mode.signature) {
            append(i)
        }
    }

    private fun FirDeclaration.visitParent() {
        val (parentPackageFqName, parentClassId) = when (this) {
            is FirCallableDeclaration -> this.containingClassLookupTag()?.classId?.let { it.packageFqName to it } ?: return
            is FirClassLikeDeclaration -> this.symbol.classId.let { it.packageFqName to it.outerClassId }
            else -> return
        }
        if (parentClassId != null && !parentClassId.isLocal) {
            val parentClassLike = this.moduleData.session.symbolProvider.getClassLikeSymbolByClassId(parentClassId)?.fir
                ?: error("Attempt to find parent ($parentClassId) for probably-local declaration!")
            if (parentClassLike is FirRegularClass || parentClassLike is FirTypeAlias) {
                parentClassLike.accept(this@FirJvmMangleComputer, false)
            } else {
                error("Strange class-like declaration: ${parentClassLike.render()}")
            }
        } else if (parentClassId == null && !parentPackageFqName.isRoot) {
            builder.appendName(parentPackageFqName.asString())
        }
    }

    private fun FirDeclaration.mangleSimpleDeclaration(name: String) {
        val l = builder.length
        visitParent()

        if (builder.length != l) {
            builder.appendName(MangleConstant.FQN_SEPARATOR)
        }

        builder.appendName(name)
    }

    private fun FirFunction.mangleFunction(isCtor: Boolean, isStatic: Boolean, container: FirDeclaration) {

        isRealExpect = isRealExpect || (this as? FirMemberDeclaration)?.isExpect == true

        if (container is FirMemberDeclaration) {
            typeParameterContainer.add(container)
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
            mangleValueParameter(this, it)
        }
        (container as? FirTypeParametersOwner)?.typeParameters?.withIndex()?.toList().orEmpty()
            .collectForMangler(builder, MangleConstant.TYPE_PARAMETERS) { (index, typeParameter) ->
                mangleTypeParameter(this, typeParameter, index)
            }

        if (!isCtor && !returnTypeRef.isUnit && addReturnType()) {
            mangleType(builder, returnTypeRef.coneType, moduleData.session)
        }
    }

    private fun FirTypeParameter.effectiveParent(): FirMemberDeclaration {
        for (parent in typeParameterContainer) {
            if (this in parent.typeParameters) {
                return parent
            }
            if (parent is FirCallableDeclaration) {
                val overriddenFir = parent.originalForSubstitutionOverride
                if (overriddenFir is FirTypeParametersOwner && this in overriddenFir.typeParameters) {
                    return parent
                }
            }
        }
        throw IllegalStateException("Should not be here!")
    }

    private fun mangleValueParameter(vpBuilder: StringBuilder, param: FirValueParameter) {
        mangleType(vpBuilder, param.returnTypeRef.coneType, param.moduleData.session)

        if (param.isVararg) {
            vpBuilder.appendSignature(MangleConstant.VAR_ARG_MARK)
        }
    }

    private fun mangleTypeParameter(tpBuilder: StringBuilder, param: FirTypeParameter, index: Int) {
        tpBuilder.appendSignature(index)
        tpBuilder.appendSignature(MangleConstant.UPPER_BOUND_SEPARATOR)

        param.bounds.map { it.coneType }.collectForMangler(tpBuilder, MangleConstant.UPPER_BOUNDS) {
            mangleType(this, it, param.moduleData.session)
        }
    }

    private fun StringBuilder.mangleTypeParameterReference(typeParameter: FirTypeParameter) {
        val parent = typeParameter.effectiveParent()
        val ci = typeParameterContainer.indexOf(parent)
        require(ci >= 0) { "No type container found for ${typeParameter.render()}" }
        appendSignature(ci)
        appendSignature(MangleConstant.INDEX_SEPARATOR)
        appendSignature(parent.typeParameters.indexOf(typeParameter))
    }

    private fun mangleType(tBuilder: StringBuilder, type: ConeKotlinType, declarationSiteSession: FirSession) {
        when (type) {
            is ConeLookupTagBasedType -> {
                when (val symbol = type.lookupTag.toSymbol(declarationSiteSession)) {
                    is FirTypeAliasSymbol -> {
                        mangleType(tBuilder, type.fullyExpandedType(declarationSiteSession), declarationSiteSession)
                        return
                    }

                    is FirClassSymbol -> symbol.fir.accept(copy(MangleMode.FQNAME), false)
                    is FirTypeParameterSymbol -> tBuilder.mangleTypeParameterReference(symbol.fir)
                    // This is performed for a case with invisible class-like symbol in fake override
                    null -> (type.lookupTag as? ConeClassLikeLookupTag)?.let {
                        tBuilder.append(it.classId)
                    }
                }

                type.typeArguments.asList().ifNotEmpty {
                    collectForMangler(tBuilder, MangleConstant.TYPE_ARGUMENTS) { arg ->
                        when (arg) {
                            is ConeStarProjection -> appendSignature(MangleConstant.STAR_MARK)
                            is ConeKotlinTypeProjection -> {
                                if (arg.kind != ProjectionKind.INVARIANT) {
                                    appendSignature(arg.kind.name.toLowerCaseAsciiOnly())
                                    appendSignature(MangleConstant.VARIANCE_SEPARATOR)
                                }

                                mangleType(this, arg.type, declarationSiteSession)
                            }
                        }
                    }
                }

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

    override fun visitElement(element: FirElement, data: Boolean) = error("unexpected element ${element.render()}")

    override fun visitRegularClass(regularClass: FirRegularClass, data: Boolean) {
        isRealExpect = isRealExpect or regularClass.isExpect
        typeParameterContainer.add(regularClass)
        regularClass.mangleSimpleDeclaration(regularClass.name.asString())
    }

    override fun visitAnonymousObject(anonymousObject: FirAnonymousObject, data: Boolean) {
        anonymousObject.mangleSimpleDeclaration("<anonymous>")
    }

    override fun visitVariable(variable: FirVariable, data: Boolean) {
        isRealExpect = isRealExpect or variable.isExpect
        typeParameterContainer.add(variable)
        variable.visitParent()

        val isStaticProperty = variable.isStatic
        if (isStaticProperty) {
            builder.appendSignature(MangleConstant.STATIC_MEMBER_MARK)
        }

        variable.receiverParameter?.typeRef?.let {
            builder.appendSignature(MangleConstant.EXTENSION_RECEIVER_PREFIX)
            mangleType(builder, it.coneType, variable.moduleData.session)
        }

        variable.typeParameters.withIndex().toList().collectForMangler(builder, MangleConstant.TYPE_PARAMETERS) { (index, typeParameter) ->
            mangleTypeParameter(this, typeParameter.symbol.fir, index)
        }

        builder.append(variable.name.asString())
    }

    override fun visitProperty(property: FirProperty, data: Boolean) {
        visitVariable(property, data)
    }

    override fun visitField(field: FirField, data: Boolean) {
        if (field is FirJavaField) {
            field.mangleSimpleDeclaration(field.name.asString())
        } else {
            visitVariable(field, data)
        }
    }

    override fun visitEnumEntry(enumEntry: FirEnumEntry, data: Boolean) {
        enumEntry.mangleSimpleDeclaration(enumEntry.name.asString())
    }

    override fun visitTypeAlias(typeAlias: FirTypeAlias, data: Boolean) =
        typeAlias.mangleSimpleDeclaration(typeAlias.name.asString())

    override fun visitSimpleFunction(simpleFunction: FirSimpleFunction, data: Boolean) {
        isRealExpect = isRealExpect || simpleFunction.isExpect
        val isStatic = simpleFunction.isStatic
        simpleFunction.mangleFunction(false, isStatic, simpleFunction)
    }

    override fun visitConstructor(constructor: FirConstructor, data: Boolean) =
        constructor.mangleFunction(isCtor = true, isStatic = false, constructor)

    override fun visitPropertyAccessor(propertyAccessor: FirPropertyAccessor, data: Boolean) {
        if (propertyAccessor is FirSyntheticPropertyAccessor) {
            // No need to distinguish between the accessor and its delegate.
            visitSimpleFunction(propertyAccessor.delegate, data)
        } else {
            propertyAccessor.mangleFunction(isCtor = false, propertyAccessor.isStatic, propertyAccessor.propertySymbol.fir)
        }
    }

    override fun computeMangle(declaration: FirDeclaration): String {
        declaration.accept(this, true)
        return builder.toString()
    }
}
