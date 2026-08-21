/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.rendering

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.rendering.KaPiece
import org.jetbrains.kotlin.analysis.api.rendering.KaPieceRenderer
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOption
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOutput
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingContext
import org.jetbrains.kotlin.analysis.api.rendering.KaTextAttribute
import org.jetbrains.kotlin.analysis.api.rendering.append
import org.jetbrains.kotlin.analysis.api.rendering.render
import org.jetbrains.kotlin.analysis.api.scopes.declaredMemberScope
import org.jetbrains.kotlin.analysis.api.symbols.KaAnonymousObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.classId
import org.jetbrains.kotlin.analysis.api.types.expandedSymbol
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.analysis.api.rendering.KaRendererBuilder

internal fun KaRendererBuilder.pushClassifierRenderers() {
    push(NamedClassRenderer)
    push(AnonymousObjectRenderer)
    push(TypeAliasRenderer)
    push(PrimaryConstructorRenderer)
    push(ClassAnnotationsRenderer)
    push(SupertypeListRenderer)
    push(SupertypeRenderer)
    push(ClassBodyRenderer)
    push(EnumEntryRenderer)
}

private object NamedClassRenderer : KaPieceRenderer<KaNamedClassSymbol>(KaPiece.NamedClass) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaNamedClassSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.ClassAnnotations)
        render(value, KaPiece.SymbolModifiers)

        val classKeyword = when (value.classKind) {
            KaClassKind.INTERFACE -> KtTokens.INTERFACE_KEYWORD
            KaClassKind.OBJECT, KaClassKind.COMPANION_OBJECT -> KtTokens.OBJECT_KEYWORD
            else -> KtTokens.CLASS_KEYWORD
        }
        keyword(classKeyword, trailingSpace = false)

        val isDefaultCompanion = value.classKind == KaClassKind.COMPANION_OBJECT &&
                value.name == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT

        if (!isDefaultCompanion) {
            output.space()
            render(value, KaPiece.SymbolName)
        }

        if (value.typeParameters.isNotEmpty()) {
            render(value, KaPiece.ClassifierTypeParameterList)
        }

        if (context.valueFor(KaRenderingOption.PrimaryConstructorInClassHeader)) {
            value.primaryConstructor?.let { render(it, KaPiece.PrimaryConstructor) }
        }

        render(value, KaPiece.ClassifierWhereClause)
        render(value, KaPiece.SupertypeList)
        render(value, KaPiece.ClassBody)

        return true
    }

    context(session: KaSession)
    private val KaClassSymbol.primaryConstructor: KaConstructorSymbol?
        get() = declaredMemberScope.constructors.firstOrNull { it.isPrimary }
}

private object AnonymousObjectRenderer : KaPieceRenderer<KaAnonymousObjectSymbol>(KaPiece.AnonymousObject) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaAnonymousObjectSymbol, next: () -> Unit): Boolean {
        keyword(KtTokens.OBJECT_KEYWORD, trailingSpace = false)
        render(value, KaPiece.SupertypeList)
        render(value, KaPiece.ClassBody)
        return true
    }
}

private object TypeAliasRenderer : KaPieceRenderer<KaTypeAliasSymbol>(KaPiece.TypeAlias) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaTypeAliasSymbol, next: () -> Unit): Boolean {
        render(value to null, KaPiece.Annotations)
        render(value, KaPiece.SymbolModifiers)
        keyword(KtTokens.TYPE_ALIAS_KEYWORD)
        render(value, KaPiece.SymbolName)

        if (value.typeParameters.isNotEmpty()) {
            render(value, KaPiece.ClassifierTypeParameterList)
        }

        output.append(" = ", KaTextAttribute.Punctuation)
        render(value.expandedType, KaPiece.Type)

        return true
    }
}

private object ClassAnnotationsRenderer : KaPieceRenderer<KaNamedClassSymbol>(KaPiece.ClassAnnotations) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaNamedClassSymbol, next: () -> Unit): Boolean {
        render(value to null, KaPiece.Annotations)
        return true
    }
}


private object SupertypeListRenderer : KaPieceRenderer<KaClassSymbol>(KaPiece.SupertypeList) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaClassSymbol, next: () -> Unit): Boolean {
        val superTypes = value.superTypes.filter { !it.isImplicitSupertype() }
        if (superTypes.isNotEmpty()) {
            output.append(" : ", KaTextAttribute.Punctuation)
            superTypes.forEachIndexed { index, superType ->
                if (index > 0) output.append(", ", KaTextAttribute.Punctuation)
                render(superType, KaPiece.Supertype)
            }
        }

        return true
    }

    context(session: KaSession)
    private fun KaType.isImplicitSupertype(): Boolean {
        return when (classId) {
            StandardClassIds.Any, StandardClassIds.Enum, StandardClassIds.Annotation -> true
            else -> false
        }
    }
}

private object SupertypeRenderer : KaPieceRenderer<KaType>(KaPiece.Supertype) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaType, next: () -> Unit): Boolean {
        render(value, KaPiece.Type)

        // A class supertype is rendered with constructor-call syntax, e.g. `Base()`; interfaces are not.
        if (value.expandedSymbol?.classKind?.isClass == true) {
            output.append("(", KaTextAttribute.Punctuation)
            output.append(")", KaTextAttribute.Punctuation)
        }
        return true
    }
}

private object ClassBodyRenderer : KaPieceRenderer<KaClassSymbol>(KaPiece.ClassBody) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaClassSymbol, next: () -> Unit): Boolean {
        val ordering = context.valueFor(KaRenderingOption.ClassMemberOrdering)

        val members = context.valueFor(KaRenderingOption.ClassMembers)(session, context, value)
            .sortedWith { first, second -> ordering(session, context, first, second) }

        if (members.isEmpty()) {
            return true
        }

        output.space()

        output.group(KaPiece.Symbol) {
            output.append("{", KaTextAttribute.Punctuation).indent()

            val enumEntries = members.filterIsInstance<KaEnumEntrySymbol>()
            val otherMembers = members.filter { it !is KaEnumEntrySymbol }

            enumEntries.forEachIndexed { index, entry ->
                output.newLine()
                render(entry, KaPiece.Symbol)
                when {
                    index < enumEntries.lastIndex -> output.append(",", KaTextAttribute.Punctuation)
                    otherMembers.isNotEmpty() -> output.append(";", KaTextAttribute.Punctuation)
                }
            }

            val extraLineBetweenMembers = context.valueFor(KaRenderingOption.ExtraLineBetweenMembers)
            otherMembers.forEachIndexed { index, member ->
                if (extraLineBetweenMembers && (enumEntries.isNotEmpty() || index > 0)) {
                    output.newLine()
                }
                output.newLine()
                render(member, KaPiece.Symbol)
            }

            output.unindent().newLine()
            output.append("}", KaTextAttribute.Punctuation)
        }

        return true
    }
}

private object PrimaryConstructorRenderer : KaPieceRenderer<KaConstructorSymbol>(KaPiece.PrimaryConstructor) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaConstructorSymbol, next: () -> Unit): Boolean {
        // The `constructor` keyword is rendered when the primary constructor carries its own annotations, or a custom visibility.
        if (value.annotations.isNotEmpty() || visibilityModifier(value) != null) {
            output.space()
            render(value to null, KaPiece.Annotations)
            render(value, KaPiece.SymbolModifiers)
            keyword(KtTokens.CONSTRUCTOR_KEYWORD, trailingSpace = false)
        }

        val parameters = value.valueParameters
        if (parameters.isNotEmpty()) {
            // Unlike a function, a class without primary constructor parameters is rendered without parentheses, as in `class Foo`.
            render(parameters, KaPiece.ValueParameterList)
        }

        return true
    }
}

private object EnumEntryRenderer : KaPieceRenderer<KaEnumEntrySymbol>(KaPiece.EnumEntry) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaEnumEntrySymbol, next: () -> Unit): Boolean {
        render(value to null, KaPiece.Annotations)
        render(value, KaPiece.SymbolModifiers)
        render(value, KaPiece.SymbolName)

        // Render the anonymous object body of the entry, e.g. `ENTRY { override fun foo() }`.
        value.initializer?.let { initializer ->
            render(initializer, KaPiece.ClassBody)
        }

        return true
    }
}
