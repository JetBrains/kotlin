/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.backend.jvm

import org.jetbrains.kotlin.backend.common.serialization.mangle.KotlinMangleComputer
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleConstant
import org.jetbrains.kotlin.backend.common.serialization.mangle.MangleMode
import org.jetbrains.kotlin.backend.common.serialization.mangle.collectForMangler
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.resolve.firSymbolProvider
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirTypeParameterSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import java.lang.IllegalStateException

open class FirJvmMangleComputer(
    private val builder: StringBuilder,
    private val mode: MangleMode,
    private val session: FirSession
) : FirVisitor<Unit, Boolean>(), KotlinMangleComputer<FirDeclaration> {

    private val typeParameterContainer = ArrayList<FirMemberDeclaration>(4)

    private var isRealExpect = false

    open fun FirFunction<*>.platformSpecificFunctionName(): String? = null

    open fun FirFunction<*>.specialValueParamPrefix(param: FirValueParameter): String = ""

    private fun addReturnType(): Boolean = false

    override fun copy(newMode: MangleMode): FirJvmMangleComputer =
        FirJvmMangleComputer(builder, newMode, session)

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
            is FirCallableDeclaration<*> -> this.symbol.callableId.let { it.packageName to it.classId }
            is FirClassLikeDeclaration<*> -> this.symbol.classId.let { it.packageFqName to it.outerClassId }
            else -> return
        }
        if (parentClassId != null && !parentClassId.isLocal) {
            val parentClassLike = session.firSymbolProvider.getClassLikeSymbolByFqName(parentClassId)?.fir
                ?: throw AssertionError("Attempt to find parent ($parentClassId) for probably-local declaration!")
            if (parentClassLike is FirRegularClass || parentClassLike is FirTypeAlias) {
                parentClassLike.accept(this@FirJvmMangleComputer, false)
            } else {
                throw AssertionError("Strange class-like declaration: ${parentClassLike.render()}")
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

    private fun FirFunction<*>.mangleFunction(isCtor: Boolean, isStatic: Boolean, container: FirDeclaration) {

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

        val name = (this as? FirSimpleFunction)?.name ?: Name.special("<anonymous>")
        builder.append(name.asString())

        mangleSignature(isCtor, isStatic)
    }

    private fun FirFunction<*>.mangleSignature(isCtor: Boolean, isStatic: Boolean) {
        if (!mode.signature) {
            return
        }

        if (isStatic) {
            builder.appendSignature(MangleConstant.STATIC_MEMBER_MARK)
        }

        receiverTypeRef?.let {
            builder.appendSignature(MangleConstant.EXTENSION_RECEIVER_PREFIX)
            mangleType(builder, it.coneTypeUnsafe())
        }

        valueParameters.collectForMangler(builder, MangleConstant.VALUE_PARAMETERS) {
            appendSignature(specialValueParamPrefix(it))
            mangleValueParameter(this, it)
        }
        typeParameters.filterIsInstance<FirTypeParameter>().withIndex().toList()
            .collectForMangler(builder, MangleConstant.TYPE_PARAMETERS) { (index, typeParameter) ->
                mangleTypeParameter(this, typeParameter, index)
            }

        if (!isCtor && !returnTypeRef.isUnit && addReturnType()) {
            mangleType(builder, returnTypeRef.coneTypeUnsafe())
        }
    }

    private fun FirTypeParameter.effectiveParent(): FirMemberDeclaration {
        for (parent in typeParameterContainer) {
            if (this in parent.typeParameters) {
                return parent
            }
            if (parent is FirCallableDeclaration<*>) {
                val overriddenSymbol = parent.symbol.overriddenSymbol
                if (overriddenSymbol != null) {
                    val fir = overriddenSymbol.fir
                    if (fir is FirTypeParametersOwner && this in fir.typeParameters) {
                        return parent
                    }
                }
            }
        }
        throw IllegalStateException("Should not be here!")
    }

    private fun mangleValueParameter(vpBuilder: StringBuilder, param: FirValueParameter) {
        mangleType(vpBuilder, param.returnTypeRef.coneTypeUnsafe())

        if (param.isVararg) {
            vpBuilder.appendSignature(MangleConstant.VAR_ARG_MARK)
        }
    }

    private fun mangleTypeParameter(tpBuilder: StringBuilder, param: FirTypeParameter, index: Int) {
        tpBuilder.appendSignature(index)
        tpBuilder.appendSignature(MangleConstant.UPPER_BOUND_SEPARATOR)

        param.bounds.map { it.coneTypeUnsafe<ConeKotlinType>() }.collectForMangler(tpBuilder, MangleConstant.UPPER_BOUNDS) {
            mangleType(this, it)
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

    private fun mangleType(tBuilder: StringBuilder, type: ConeKotlinType) {
        when (type) {
            is ConeLookupTagBasedType -> {
                when (val symbol = type.lookupTag.toSymbol(session)) {
                    is FirClassSymbol -> symbol.fir.accept(copy(MangleMode.FQNAME), false)
                    is FirTypeParameterSymbol -> tBuilder.mangleTypeParameterReference(symbol.fir)
                }

                type.typeArguments.asList().ifNotEmpty {
                    collectForMangler(tBuilder, MangleConstant.TYPE_ARGUMENTS) { arg ->
                        when (arg) {
                            is ConeStarProjection -> appendSignature(MangleConstant.STAR_MARK)
                            is ConeKotlinTypeProjection -> {
                                if (arg.kind != ProjectionKind.INVARIANT) {
                                    appendSignature(arg.kind.name.toLowerCase())
                                    appendSignature(MangleConstant.VARIANCE_SEPARATOR)
                                }

                                mangleType(this, arg.type)
                            }
                        }
                    }
                }

                if (type.isMarkedNullable) {
                    tBuilder.appendSignature(MangleConstant.Q_MARK)
                }
            }
            is ConeFlexibleType -> {
                // TODO: is that correct way to mangle flexible type?
                with(MangleConstant.FLEXIBLE_TYPE) {
                    tBuilder.appendSignature(prefix)
                    mangleType(tBuilder, type.lowerBound)
                    tBuilder.appendSignature(separator)
                    mangleType(tBuilder, type.upperBound)
                    tBuilder.appendSignature(suffix)
                }
            }
            is ConeDefinitelyNotNullType -> {
                // E.g. not-null type parameter in Java
                mangleType(tBuilder, type.original)
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

    override fun visitProperty(property: FirProperty, data: Boolean) {
        isRealExpect = isRealExpect or property.isExpect
        typeParameterContainer.add(property)
        property.visitParent()

        val isStaticProperty = property.isStatic
        if (isStaticProperty) {
            builder.appendSignature(MangleConstant.STATIC_MEMBER_MARK)
        }

        property.receiverTypeRef?.let {
            builder.appendSignature(MangleConstant.EXTENSION_RECEIVER_PREFIX)
            mangleType(builder, it.coneTypeUnsafe())
        }

        property.typeParameters.withIndex().toList().collectForMangler(builder, MangleConstant.TYPE_PARAMETERS) { (index, typeParameter) ->
            mangleTypeParameter(this, typeParameter, index)
        }

        builder.append(property.name.asString())
    }

    override fun visitField(field: FirField, data: Boolean) =
        field.mangleSimpleDeclaration(field.name.asString())

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

    override fun computeMangle(declaration: FirDeclaration): String {
        declaration.accept(this, true)
        return builder.toString()
    }
}
