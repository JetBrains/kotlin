/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.renderer

import org.jetbrains.kotlin.analysis.api.components.KtTypeRendererOptions
import org.jetbrains.kotlin.analysis.low.level.api.fir.api.tryCollectDesignation
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.containingClass
import org.jetbrains.kotlin.fir.containingClassForLocal
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.isInner
import org.jetbrains.kotlin.fir.declarations.utils.isLocal
import org.jetbrains.kotlin.fir.expressions.FirAnnotation
import org.jetbrains.kotlin.fir.renderWithType
import org.jetbrains.kotlin.fir.resolve.diagnostics.ConeUnresolvedError
import org.jetbrains.kotlin.fir.resolve.toFirRegularClass
import org.jetbrains.kotlin.fir.resolve.toSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.LookupTagInternals
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.renderer.render
import org.jetbrains.kotlin.utils.addToStdlib.applyIf

internal class ConeTypeIdeRenderer(
    private val session: FirSession,
    private val options: KtTypeRendererOptions,
) {
    companion object {
        const val ERROR_TYPE_TEXT = "ERROR_TYPE"
    }

    private fun StringBuilder.appendError(message: String? = null) {
        append(ERROR_TYPE_TEXT)
        if (message != null) append(" <$message>")
    }

    private fun StringBuilder.renderAnnotationList(type: ConeKotlinType) {
        if (options.renderTypeAnnotations) {
            renderAnnotations(this@ConeTypeIdeRenderer, type.customAnnotations, session, isSingleLineAnnotations = true)
        }
    }

    fun renderType(type: ConeTypeProjection): String = buildString {
        when (type) {
            is ConeErrorType -> {
                renderErrorType(type)
            }
            //is Dynamic??? -> append("dynamic")
            is ConeClassLikeType -> {
                if (options.renderFunctionType && shouldRenderAsPrettyFunctionType(type)) {
                    renderAnnotationList(type)
                    renderFunctionType(type)
                } else {
                    renderAnnotationList(type)
                    renderTypeConstructorAndArguments(type)
                }
            }
            is ConeTypeParameterType -> {
                renderAnnotationList(type)
                append(type.lookupTag.name.asString())
                renderNullability(type.type)
            }
            is ConeIntersectionType -> {
                renderAnnotationList(type)
                type.intersectedTypes.joinTo(this, "&", prefix = "(", postfix = ")") {
                    renderType(it)
                }
                renderNullability(type.type)
            }
            is ConeFlexibleType -> {
                renderAnnotationList(type)
                append(renderFlexibleType(renderType(type.lowerBound), renderType(type.upperBound)))
            }
            is ConeCapturedType -> {
                renderAnnotationList(type)
                append(type.render())
                renderNullability(type.type)
            }
            is ConeDefinitelyNotNullType -> {
                renderAnnotationList(type)
                append(renderType(type.original))
                append("!!")
            }
            else -> appendError("Unexpected cone type ${type::class.qualifiedName}")
        }
    }

    private fun StringBuilder.renderErrorType(type: ConeErrorType) {
        val diagnostic = type.diagnostic
        if (options.renderUnresolvedTypeAsResolved && diagnostic is ConeUnresolvedError) {
            val qualifierRendered = diagnostic.qualifier?.let { FqName(it).render() }.orEmpty()
            append(qualifierRendered)
        } else {
            appendError(diagnostic.reason)
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

    private fun FirRegularClass.collectForLocal(): List<FirClassLikeDeclaration> {
        require(isLocal)
        var containingClassLookUp = containingClassForLocal()
        val designation = mutableListOf<FirClassLikeDeclaration>(this)
        @OptIn(LookupTagInternals::class)
        while (containingClassLookUp != null && containingClassLookUp.classId.isLocal) {
            val currentClass = containingClassLookUp.toFirRegularClass(moduleData.session) ?: break
            designation.add(currentClass)
            containingClassLookUp = currentClass.containingClassForLocal()
        }
        return designation
    }

    private fun collectDesignationPathForLocal(declaration: FirDeclaration): List<FirDeclaration>? {
        @OptIn(LookupTagInternals::class)
        val containingClass = when (declaration) {
            is FirCallableDeclaration -> declaration.containingClass()?.toFirRegularClass(declaration.moduleData.session)
            is FirAnonymousObject -> return listOf(declaration)
            is FirClassLikeDeclaration -> declaration.let {
                if (!declaration.isLocal) return null
                (it as? FirRegularClass)?.containingClassForLocal()?.toFirRegularClass(declaration.moduleData.session)
            }
            else -> error("Invalid declaration ${declaration.renderWithType()}")
        } ?: return listOf(declaration)

        return if (containingClass.isLocal) {
            containingClass.collectForLocal().reversed()
        } else null
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

        val designation = classSymbolToRender.fir.let {
            val nonLocalDesignation = it.tryCollectDesignation()
            nonLocalDesignation?.toSequence(includeTarget = true)?.toList()
                ?: collectDesignationPathForLocal(it)
                ?: emptyList()
        }

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
            .applyIf(receiverType != null) { drop(1) }

        notNullParametersType.forEachIndexed { index, typeProjection ->
            if (index != 0) append(", ")
            append(renderTypeProjection(typeProjection))
        }

        append(") -> ")

        val returnType = type.returnType(session)
        append(renderType(returnType))

        if (needParenthesis) append(")")

        renderNullability(type)
    }
}
