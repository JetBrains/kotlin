/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.compiler.visualizer

import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirNamedReference
import org.jetbrains.kotlin.fir.FirResolvedCallableReference
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.impl.FirAbstractMemberDeclaration
import org.jetbrains.kotlin.fir.declarations.impl.FirClassImpl
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.references.FirErrorNamedReference
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.directExpansionType
import org.jetbrains.kotlin.fir.resolve.transformers.firSafeNullable
import org.jetbrains.kotlin.fir.resolve.transformers.firUnsafe
import org.jetbrains.kotlin.fir.symbols.ConeCallableSymbol
import org.jetbrains.kotlin.fir.symbols.ConeClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.ConeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirPropertySymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirVariableSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.FirVisitor
import org.jetbrains.kotlin.ir.expressions.IrConstKind
import org.jetbrains.kotlin.name.ClassId

class FirRenderer(private val firFile: FirFile) : FirVisitor<Unit, StringBuilder>() {
    private val session = firFile.session
    private val filePackage = firFile.packageFqName.toString().replace(".", "/")
    private val symbolProvider = firFile.session.getService(FirSymbolProvider::class)

    private fun FirElement.render(): String = buildString { this@render.accept(FirRenderer(firFile), this) }

    private fun removeCurrentFilePackage(fqName: String): String {
        return if (fqName.startsWith(filePackage) && !fqName.substring(filePackage.length + 1).contains("/")) {
            fqName.replaceFirst("$filePackage/", "")
        } else {
            fqName
        }
    }

    private fun ClassId.getWithoutCurrentPackage() = removeCurrentFilePackage(this.asString())

    private fun <T : FirElement> renderListInTriangles(list: List<T>, data: StringBuilder, withSpace: Boolean = false) {
        if (list.isNotEmpty()) {
            list.joinTo(data, separator = ", ", prefix = "<", postfix = ">") {
                buildString { it.accept(this@FirRenderer, this) }
            }
            if (withSpace) data.append(" ")
        }
    }

    private fun visitArguments(arguments: List<FirExpression>, data: StringBuilder) {
        arguments.joinTo(data, ", ", "(", ")") {
            if (it is FirResolvedQualifier) {
                val lookupTag = (it.typeRef as FirResolvedTypeRefImpl).coneTypeSafe<ConeClassType>()?.lookupTag
                val type = lookupTag?.let {
                    symbolProvider.getSymbolByLookupTag(it)?.firSafeNullable<FirClass>()?.superTypeRefs?.first()?.render()
                }
                if (type != null) return@joinTo type
            }
            it.typeRef.render()
        }
    }

    override fun visitElement(element: FirElement, data: StringBuilder) {
        element.acceptChildren(this, data)
    }

    override fun visitConstructor(constructor: FirConstructor, data: StringBuilder) {
        data.append(renderSymbol(constructor.symbol))
        visitValueParameters(constructor.valueParameters, data)
    }

    override fun visitTypeParameter(typeParameter: FirTypeParameter, data: StringBuilder) {
        data.append(typeParameter.name)
        val bounds = typeParameter.bounds.filterNot { it.render() == "kotlin/Any?" }
        if (bounds.isNotEmpty()) {
            data.append(" : ")
            bounds.joinTo(data, separator = ", ") {
                buildString { it.accept(this@FirRenderer, this) }
            }
        }
    }

    override fun visitProperty(property: FirProperty, data: StringBuilder) {
        data.append(property.returnTypeRef.render())
    }

    private fun visitValueParameters(valueParameters: List<FirValueParameter>, data: StringBuilder) {
        valueParameters.joinTo(data, separator = ", ", prefix = "(", postfix = ")") {
            buildString { it.accept(this@FirRenderer, this) }
        }
    }

    override fun visitValueParameter(valueParameter: FirValueParameter, data: StringBuilder) {
        if (valueParameter.isVararg) {
            data.append("vararg ")
        }
        valueParameter.returnTypeRef.coneTypeSafe<ConeClassType>()?.arrayElementType(session)?.let { data.append(it.render()) }
            ?: valueParameter.returnTypeRef.accept(this, data)
        valueParameter.defaultValue?.let { data.append(" = ...") }
    }

    override fun <F : FirVariable<F>> visitVariable(variable: FirVariable<F>, data: StringBuilder) {
        data.append(variable.returnTypeRef.render())
    }

    override fun visitNamedReference(namedReference: FirNamedReference, data: StringBuilder) {
        if (namedReference is FirErrorNamedReference) {
            data.append("[ERROR : ${namedReference.errorReason}]")
            return
        }
        super.visitNamedReference(namedReference, data)
    }

    override fun visitResolvedCallableReference(resolvedCallableReference: FirResolvedCallableReference, data: StringBuilder) {
        val symbol = resolvedCallableReference.coneSymbol
        data.append(renderSymbol(symbol))
    }

    override fun visitAnnotationCall(annotationCall: FirAnnotationCall, data: StringBuilder) {
        data.append(annotationCall.annotationTypeRef.render())
    }

    override fun visitDelegatedConstructorCall(delegatedConstructorCall: FirDelegatedConstructorCall, data: StringBuilder) {
        val coneClassType = delegatedConstructorCall.constructedTypeRef.coneTypeSafe<ConeClassLikeType>()
        if (coneClassType != null) {
            data.append("constructor").append(" ")

            data.append(coneClassType.lookupTag.classId.getWithoutCurrentPackage())

            val typeParameters = symbolProvider.getSymbolByLookupTag(coneClassType.lookupTag)
                ?.firSafeNullable<FirClassImpl>()
                ?.typeParameters ?: listOf<FirTypeParameter>()
            renderListInTriangles(typeParameters, data)
            visitArguments(delegatedConstructorCall.arguments, data)
        } else {
            data.append("[ERROR : ${delegatedConstructorCall.constructedTypeRef.render()}]")
        }
    }

    override fun visitOperatorCall(operatorCall: FirOperatorCall, data: StringBuilder) {
        data.append("operator call ${operatorCall.operation}")
    }

    override fun visitTypeOperatorCall(typeOperatorCall: FirTypeOperatorCall, data: StringBuilder) {
        //skip rendering for as/as?/is/!is
    }

    override fun visitFunctionCall(functionCall: FirFunctionCall, data: StringBuilder) {
        when (val callee = functionCall.calleeReference) {
            is FirResolvedCallableReference -> {
                if (callee.coneSymbol is FirConstructorSymbol) {
                    data.append(renderSymbol(callee.coneSymbol))
                    visitArguments(functionCall.arguments, data)
                } else {
                    data.append("fun ")
                    val firFunction = callee.coneSymbol.firSafeNullable<FirAbstractMemberDeclaration>()
                    firFunction?.let { renderListInTriangles(it.typeParameters, data, true) }

                    data.append(renderSymbol(callee.coneSymbol))
                    renderListInTriangles(functionCall.typeArguments, data)
                    visitValueParameters(callee.coneSymbol.firUnsafe<FirFunction<*>>().valueParameters, data)
                    data.append(": ")
                    functionCall.typeRef.accept(this, data)
                }
            }
            is FirErrorNamedReference -> data.append("[ERROR : ${callee.errorReason}]")
        }
    }

    override fun <T> visitConstExpression(constExpression: FirConstExpression<T>, data: StringBuilder) {
        if (constExpression.kind != IrConstKind.String) {
            data.append(constExpression.kind)
        }
    }

    override fun visitResolvedQualifier(resolvedQualifier: FirResolvedQualifier, data: StringBuilder) {
        resolvedQualifier.classId?.let {
            val fir = symbolProvider.getClassLikeSymbolByFqName(it)?.fir
            if (fir is FirClass) {
                data.append(fir.classKind.name.toLowerCase()).append(" ")
                data.append(fir.name)
                if (fir.superTypeRefs.any { it.render() != "kotlin/Any" }) {
                    data.append(": ")
                    fir.superTypeRefs.joinTo(data, separator = ", ") { typeRef -> typeRef.render() }
                }
            }
        }
    }

    override fun visitVariableAssignment(variableAssignment: FirVariableAssignment, data: StringBuilder) {
        data.append("qualified access: \"${variableAssignment.operation}\"")
    }

    override fun visitStarProjection(starProjection: FirStarProjection, data: StringBuilder) {
        data.append("*")
    }

    override fun visitTypeProjectionWithVariance(typeProjectionWithVariance: FirTypeProjectionWithVariance, data: StringBuilder) {
        val variance = typeProjectionWithVariance.variance.label
        if (variance.isNotEmpty()) data.append("$variance ")
        typeProjectionWithVariance.typeRef.accept(this, data)
    }

    override fun visitDelegatedTypeRef(delegatedTypeRef: FirDelegatedTypeRef, data: StringBuilder) {
        delegatedTypeRef.typeRef.accept(this, data)
        data.append(" by ")
        delegatedTypeRef.delegate?.accept(this, data)
    }

    override fun visitResolvedTypeRef(resolvedTypeRef: FirResolvedTypeRef, data: StringBuilder) {
        val coneType = resolvedTypeRef.type
        data.append(removeCurrentFilePackage(coneType.render()))
        if (coneType is ConeAbbreviatedType) {
            val original = coneType.directExpansionType(session)
            original?.let { data.append(" /* = ${it.render()} */") }
        }
    }

    override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef, data: StringBuilder) {
        data.append("[ERROR : ${errorTypeRef.reason}]")
    }

    override fun visitResolvedFunctionTypeRef(resolvedFunctionTypeRef: FirResolvedFunctionTypeRef, data: StringBuilder) {
        resolvedFunctionTypeRef.receiverTypeRef?.let {
            it.accept(this, data)
            data.append(".")
        }
        visitValueParameters(resolvedFunctionTypeRef.valueParameters, data)
        data.append(" -> ")
        resolvedFunctionTypeRef.returnTypeRef.accept(this, data)
    }

    override fun visitTypeRefWithNullability(typeRefWithNullability: FirTypeRefWithNullability, data: StringBuilder) {
        if (typeRefWithNullability.isMarkedNullable) {
            data.append("?")
        }
    }

    private fun renderSymbol(symbol: ConeSymbol?): String {
        val data = StringBuilder()
        var id = when (symbol) {
            is ConeCallableSymbol -> {
                val callableId = symbol.callableId
                val idWithPackage = callableId.toString().replace("." + callableId.callableName.asString(), "")
                removeCurrentFilePackage(idWithPackage)
            }
            is ConeClassLikeSymbol -> symbol.classId.getWithoutCurrentPackage()
            else -> ""
        }

        if (id.startsWith("/")) {
            id = id.substring(1)
        }

        when (symbol) {
            is FirNamedFunctionSymbol -> {
                val callableName = symbol.callableId.callableName
                val receiverType = symbol.fir.receiverTypeRef
                if (receiverType == null) {
                    if (symbol.callableId.className == null) {
                        data.append(id)
                    } else {
                        data.append("($id).$callableName")
                    }
                } else {
                    data.append("${receiverType.render()}.$callableName")
                }
            }
            is FirPropertySymbol -> {
                data.append(if (symbol.fir.isVar) "var" else "val").append(" ")
                renderListInTriangles(symbol.fir.typeParameters, data, withSpace = true)

                val receiver = symbol.fir.receiverTypeRef?.render()
                if (receiver != null) {
                    data.append(receiver).append(".")
                } else if (id != symbol.callableId.callableName.asString()) {
                    data.append("($id)").append(".")
                }

                data.append(symbol.callableId.callableName).append(": ")
                symbol.fir.returnTypeRef.accept(this, data)
            }
            is FirVariableSymbol<*> -> {
                if (symbol.fir !is FirValueParameter) {
                    if (symbol.fir.isVar) data.append("var ") else if (symbol.fir.isVal) data.append("val ")
                }
                data.append(id)

                data.append(": ")
                symbol.fir.returnTypeRef.accept(this, data)
            }
            is FirConstructorSymbol -> {
                data.append("constructor ")
                val packageName = symbol.callableId.className
                data.append(packageName)
                renderListInTriangles(symbol.fir.typeParameters, data)
            }
        }
        return data.toString()
    }
}