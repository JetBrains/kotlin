/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.frontend.api.fir.renderer

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirRegularClass
import org.jetbrains.kotlin.fir.declarations.FirTypeParameter
import org.jetbrains.kotlin.fir.declarations.isInner
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassId
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.resolve.inference.*
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.idea.asJava.applyIf
import org.jetbrains.kotlin.idea.fir.low.level.api.api.collectDesignation
import org.jetbrains.kotlin.idea.frontend.api.components.KtTypeRendererOptions
import org.jetbrains.kotlin.name.StandardClassIds

internal class ConeTypeIdeRenderer(
    private val session: FirSession,
    private val options: KtTypeRendererOptions,
) {
    companion object {
        const val ERROR_TYPE_TEXT = "ERROR_TYPE"
    }

    private fun StringBuilder.appendError(message: String? = null) {
        append(ERROR_TYPE_TEXT)
        if (message != null) append(" $message")
    }

    private var filterExtensionFunctionType: Boolean = false

    private fun StringBuilder.renderAnnotationList(annotations: List<FirAnnotationCall>?) {
        if (annotations != null) {
            val filteredExtensionIfNeeded = annotations.applyIf(filterExtensionFunctionType) {
                annotations.filterNot { it.toAnnotationClassId() == StandardClassIds.extensionFunctionType }
            }
            renderAnnotations(this@ConeTypeIdeRenderer, filteredExtensionIfNeeded, session)
        }
    }

    fun renderType(type: ConeTypeProjection, annotations: List<FirAnnotationCall>? = null): String = buildString {

        when (type) {
            is ConeKotlinErrorType -> {
                appendError()
            }
            //is Dynamic??? -> append("dynamic")
            is ConeClassLikeType -> {
                if (options.renderFunctionType && shouldRenderAsPrettyFunctionType(type)) {
                    val oldFilterExtensionFunctionType = filterExtensionFunctionType
                    filterExtensionFunctionType = true
                    renderAnnotationList(annotations)
                    renderFunctionType(type)
                    filterExtensionFunctionType = oldFilterExtensionFunctionType
                } else {
                    renderAnnotationList(annotations)
                    renderTypeConstructorAndArguments(type)
                }
            }
            is ConeTypeParameterType -> {
                renderAnnotationList(annotations)
                append(type.lookupTag.name.asString())
                renderNullability(type.type)
            }
            is ConeIntersectionType -> {
                renderAnnotationList(annotations)
                type.intersectedTypes.joinTo(this, "&", prefix = "(", postfix = ")") {
                    renderType(it)
                }
                renderNullability(type.type)
            }
            is ConeFlexibleType -> {
                renderAnnotationList(annotations)
                append(renderFlexibleType(renderType(type.lowerBound), renderType(type.upperBound)))
            }
            else -> appendError("Unexpected cone type ${type::class.qualifiedName}")
        }
    }

    private fun StringBuilder.renderNullability(type: ConeKotlinType) {
        if (type.nullability == ConeNullability.NULLABLE) {
            append("?")
        }
    }


    fun shouldRenderAsPrettyFunctionType(type: ConeKotlinType): Boolean {
        return type.type.isBuiltinFunctionalType(session) && type.typeArguments.none { it.kind == ProjectionKind.STAR }
    }

    private fun differsOnlyInNullability(lower: String, upper: String) =
        lower == upper.replace("?", "") || upper.endsWith("?") && ("$lower?") == upper || "($lower)?" == upper


    private fun renderFlexibleType(lowerRendered: String, upperRendered: String): String {
        if (differsOnlyInNullability(lowerRendered, upperRendered)) {
            if (upperRendered.startsWith("(")) {
                // the case of complex type, e.g. (() -> Unit)?
                return "($lowerRendered)!"
            }
            return "$lowerRendered!"
        }

        val kotlinCollectionsPrefix = "kotlin.collections."
        val mutablePrefix = "Mutable"
        // java.util.List<Foo> -> (Mutable)List<Foo!>!
        val simpleCollection = replacePrefixes(
            lowerRendered,
            kotlinCollectionsPrefix + mutablePrefix,
            upperRendered,
            kotlinCollectionsPrefix,
            "$kotlinCollectionsPrefix($mutablePrefix)"
        )
        if (simpleCollection != null) return simpleCollection
        // java.util.Map.Entry<Foo, Bar> -> (Mutable)Map.(Mutable)Entry<Foo!, Bar!>!
        val mutableEntry = replacePrefixes(
            lowerRendered,
            kotlinCollectionsPrefix + "MutableMap.MutableEntry",
            upperRendered,
            kotlinCollectionsPrefix + "Map.Entry",
            "$kotlinCollectionsPrefix(Mutable)Map.(Mutable)Entry"
        )
        if (mutableEntry != null) return mutableEntry

        val kotlinPrefix = "kotlin."
        // Foo[] -> Array<(out) Foo!>!
        val array = replacePrefixes(
            lowerRendered,
            kotlinPrefix + "Array<",
            upperRendered,
            kotlinPrefix + "Array<out ",
            kotlinPrefix + "Array<(out) "
        )
        if (array != null) return array

        return "($lowerRendered..$upperRendered)"
    }

    private fun replacePrefixes(
        lowerRendered: String,
        lowerPrefix: String,
        upperRendered: String,
        upperPrefix: String,
        foldedPrefix: String
    ): String? {
        if (lowerRendered.startsWith(lowerPrefix) && upperRendered.startsWith(upperPrefix)) {
            val lowerWithoutPrefix = lowerRendered.substring(lowerPrefix.length)
            val upperWithoutPrefix = upperRendered.substring(upperPrefix.length)
            val flexibleCollectionName = foldedPrefix + lowerWithoutPrefix

            if (lowerWithoutPrefix == upperWithoutPrefix) return flexibleCollectionName

            if (differsOnlyInNullability(lowerWithoutPrefix, upperWithoutPrefix)) {
                return "$flexibleCollectionName!"
            }
        }
        return null
    }

    private fun StringBuilder.renderTypeConstructorAndArguments(type: ConeClassLikeType) {
        fun renderTypeArguments(typeArguments: Array<out ConeTypeProjection>, range: IntRange) {
            if (range.any()) {
                typeArguments.slice(range).joinTo(this, ", ", prefix = "<", postfix = ">") {
                    renderTypeProjection(it)
                }
            }
        }

        val classSymbolToRender = type.lookupTag.toSymbol(session)
        if (classSymbolToRender == null) {
            appendError("Unresolved type")
            return
        }

        if (!options.shortQualifiedNames && !classSymbolToRender.classId.isLocal) {
            val packageName = classSymbolToRender.classId.packageFqName.asString()
            if (packageName.isNotEmpty()) {
                append(packageName).append(".")
            }
        }

        if (classSymbolToRender !is FirRegularClassSymbol) {
            append(classSymbolToRender.classId.shortClassName)
            if (type.typeArguments.any()) {
                type.typeArguments.joinTo(this, ", ", prefix = "<", postfix = ">") {
                    renderTypeProjection(it)
                }
            }
            return
        }

        val classToRender = classSymbolToRender.fir
        val designation = classToRender.collectDesignation()
            .toSequence(includeTarget = true)
            .toList()

        var typeParametersLeft = type.typeArguments.count()
        fun needToRenderTypeParameters(index: Int): Boolean {
            if (typeParametersLeft <= 0) return false
            return index == designation.lastIndex ||
                    (designation[index] as? FirRegularClass)?.isInner == true ||
                    (designation[index + 1] as? FirRegularClass)?.isInner == true
        }

        designation.filterIsInstance<FirRegularClass>().forEachIndexed { index, currentClass ->
            if (index != 0) append(".")
            append(currentClass.name)

            if (needToRenderTypeParameters(index)) {
                val typeParametersCount = currentClass.typeParameters.count { it is FirTypeParameter }
                val begin = typeParametersLeft - typeParametersCount
                val end = typeParametersLeft
                check(begin >= 0)
                typeParametersLeft -= typeParametersCount
                renderTypeArguments(type.typeArguments, begin until end)
            }
        }

        renderNullability(type)
    }

    private fun renderTypeProjection(typeProjection: ConeTypeProjection): String {
        val type = typeProjection.type?.let(::renderType) ?: "???"
        return when (typeProjection.kind) {
            ProjectionKind.STAR -> "*"
            ProjectionKind.IN -> "in $type"
            ProjectionKind.OUT -> "out $type"
            ProjectionKind.INVARIANT -> type
        }
    }

    private fun StringBuilder.renderFunctionType(type: ConeClassLikeType) {
        val lengthBefore = length
        val hasAnnotations = length != lengthBefore

        val isSuspend = type.isSuspendFunctionType(session)
        val isNullable = type.isMarkedNullable

        val receiverType = type.receiverType(session)

        val needParenthesis = isNullable || (hasAnnotations && receiverType != null)
        if (needParenthesis) {
            if (isSuspend) {
                insert(lengthBefore, '(')
            } else {
                if (hasAnnotations) {
                    check(last() == ' ')
                    if (get(lastIndex - 1) != ')') {
                        // last annotation rendered without parenthesis - need to add them otherwise parsing will be incorrect
                        insert(lastIndex, "()")
                    }
                }

                append("(")
            }
        }

        if (isSuspend) {
            append("suspend")
            append(" ")
        }

        if (receiverType != null) {
            val surroundReceiver = shouldRenderAsPrettyFunctionType(receiverType) &&
                    !receiverType.isMarkedNullable ||
                    receiverType.isSuspendFunctionType(session)
            if (surroundReceiver) {
                append("(")
            }
            append(renderType(receiverType))
            if (surroundReceiver) {
                append(")")
            }
            append(".")
        }

        append("(")

        val notNullParametersType = type
            .valueParameterTypesIncludingReceiver(session)
            .filterNotNull()
            .applyIf(receiverType != null) { drop(1) }

        notNullParametersType.forEachIndexed { index, typeProjection ->
            if (index != 0) append(", ")
            append(renderTypeProjection(typeProjection))
        }

        append(") -> ")

        val returnType = type.returnType(session)
        if (returnType != null) {
            append(renderType(returnType))
        } else {
            appendError()
        }

        if (needParenthesis) append(")")

        renderNullability(type)
    }
}