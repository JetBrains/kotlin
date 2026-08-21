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
import org.jetbrains.kotlin.analysis.api.symbols.KaAnonymousFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaAnonymousObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaContextParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaDeclarationSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaEnumEntrySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaJavaFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaLocalVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPackageSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaReceiverParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSamConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolModality
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolVisibility
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeAliasSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaTypeParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaValueParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.containingDeclaration
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.addRemoveModifier.sortModifiers
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.kotlin.analysis.api.rendering.KaRendererBuilder

internal fun KaRendererBuilder.pushSymbolRenderers() {
    push(SymbolRenderer)
    push(SymbolNameRenderer)
    push(SymbolModifiersRenderer)
}

private object SymbolRenderer : KaPieceRenderer<KaSymbol>(KaPiece.Symbol) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaSymbol, next: () -> Unit): Boolean {
        when (value) {
            is KaNamedFunctionSymbol -> render(value, KaPiece.NamedFunction)
            is KaConstructorSymbol -> render(value, KaPiece.Constructor)
            is KaPropertyAccessorSymbol -> render(value, KaPiece.PropertyAccessor)
            is KaSamConstructorSymbol -> render(value, KaPiece.Function)
            is KaAnonymousFunctionSymbol -> render(value, KaPiece.Function)
            is KaPropertySymbol -> render(value, KaPiece.Property)
            is KaLocalVariableSymbol -> render(value, KaPiece.LocalVariable)
            is KaJavaFieldSymbol -> render(value, KaPiece.JavaField)
            is KaBackingFieldSymbol -> render(value, KaPiece.BackingField)
            is KaEnumEntrySymbol -> render(value, KaPiece.EnumEntry)
            is KaValueParameterSymbol -> render(value, KaPiece.ValueParameter)
            is KaContextParameterSymbol -> render(value, KaPiece.ContextParameter)
            is KaReceiverParameterSymbol -> render(value, KaPiece.FunctionReceiver)
            is KaNamedClassSymbol -> render(value, KaPiece.NamedClass)
            is KaAnonymousObjectSymbol -> render(value, KaPiece.AnonymousObject)
            is KaTypeAliasSymbol -> render(value, KaPiece.TypeAlias)
            is KaTypeParameterSymbol -> render(value, KaPiece.TypeParameter)
            is KaPackageSymbol -> render(value, KaPiece.Package)
            else -> render(value, KaPiece.SymbolName)
        }
        return true
    }
}

private object SymbolNameRenderer : KaPieceRenderer<KaSymbol>(KaPiece.SymbolName) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaSymbol, next: () -> Unit): Boolean {
        when (value) {
            // A package is declared by its fully qualified name, e.g. `package org.example`.
            is KaPackageSymbol -> render(value.fqName, KaPiece.PackageName)
            is KaNamedSymbol -> identifier(value.name, value)
            else -> output.append("<symbol>", KaTextAttribute.Identifier)
        }
        return true
    }
}

private object SymbolModifiersRenderer : KaPieceRenderer<KaDeclarationSymbol>(KaPiece.SymbolModifiers) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaDeclarationSymbol, next: () -> Unit): Boolean {
        val defaultModifiers = sortModifiers(defaultModifiers(value))
        val modifiers = context.valueFor(KaRenderingOption.Modifiers)(session, context, value, defaultModifiers)
        for (modifier in modifiers) {
            keyword(modifier)
        }
        return true
    }

    /**
     * The modifiers which [symbol] carries in source code, in no particular order (the caller sorts them).
     *
     * A declaration only contributes the modifiers which are meaningful for its kind, so a constructor contributes just its visibility,
     * while a named function also contributes its modality and modifiers such as `suspend` and `operator`.
     */
    context(session: KaSession)
    private fun defaultModifiers(symbol: KaDeclarationSymbol): List<KtModifierKeywordToken> = buildList {
        when (symbol) {
            // A property accessor and a constructor only carry a visibility; their modality always follows the containing declaration.
            is KaPropertyAccessorSymbol, is KaConstructorSymbol -> {
                addIfNotNull(visibilityModifier(symbol))
            }
            is KaFunctionSymbol -> {
                addAll(commonModifiers(symbol))
                addIfNotNull(modalityModifier(symbol))
                if (symbol is KaNamedFunctionSymbol) {
                    if (symbol.isOverride) add(KtTokens.OVERRIDE_KEYWORD)
                    if (symbol.isTailRec) add(KtTokens.TAILREC_KEYWORD)
                    if (symbol.isSuspend) add(KtTokens.SUSPEND_KEYWORD)
                    if (symbol.isInline) add(KtTokens.INLINE_KEYWORD)
                    if (symbol.isInfix) add(KtTokens.INFIX_KEYWORD)
                    if (symbol.isOperator) add(KtTokens.OPERATOR_KEYWORD)
                }
            }
            is KaPropertySymbol -> {
                addAll(commonModifiers(symbol))
                addIfNotNull(modalityModifier(symbol))
                if (symbol.isOverride) add(KtTokens.OVERRIDE_KEYWORD)
                if (symbol is KaKotlinPropertySymbol) {
                    if (symbol.isConst) add(KtTokens.CONST_KEYWORD)
                    if (symbol.isLateInit) add(KtTokens.LATEINIT_KEYWORD)
                }
            }
            is KaNamedClassSymbol -> {
                addAll(commonModifiers(symbol))
                addIfNotNull(modalityModifier(symbol))

                when (symbol.classKind) {
                    KaClassKind.COMPANION_OBJECT -> add(KtTokens.COMPANION_KEYWORD)
                    KaClassKind.ENUM_CLASS -> add(KtTokens.ENUM_KEYWORD)
                    KaClassKind.ANNOTATION_CLASS -> add(KtTokens.ANNOTATION_KEYWORD)
                    else -> {}
                }

                if (symbol.isInner) add(KtTokens.INNER_KEYWORD)
                if (symbol.isData) add(KtTokens.DATA_KEYWORD)
                if (symbol.isInline) add(KtTokens.VALUE_KEYWORD)
                if (symbol.isFun) add(KtTokens.FUN_KEYWORD)
            }
            is KaTypeAliasSymbol -> {
                addAll(commonModifiers(symbol))
            }
            is KaJavaFieldSymbol -> {
                // The `static` of a Java field has no Kotlin modifier keyword, so it is rendered by `JavaFieldRenderer` instead.
                addIfNotNull(visibilityModifier(symbol))
            }
            is KaLocalVariableSymbol -> {
                if (symbol.isLateInit) add(KtTokens.LATEINIT_KEYWORD)
            }
            is KaValueParameterSymbol -> {
                if (symbol.isVararg) add(KtTokens.VARARG_KEYWORD)
                if (symbol.isCrossinline) add(KtTokens.CROSSINLINE_KEYWORD)
                if (symbol.isNoinline) add(KtTokens.NOINLINE_KEYWORD)
            }
            is KaTypeParameterSymbol -> {
                if (symbol.isReified) add(KtTokens.REIFIED_KEYWORD)
                when (symbol.variance) {
                    Variance.IN_VARIANCE -> add(KtTokens.IN_KEYWORD)
                    Variance.OUT_VARIANCE -> add(KtTokens.OUT_KEYWORD)
                    Variance.INVARIANT -> {}
                }
            }
            else -> {
                // Enum entries, backing fields, and context parameters cannot carry modifiers.
            }
        }
    }

    context(session: KaSession)
    private fun commonModifiers(symbol: KaDeclarationSymbol): List<KtModifierKeywordToken> = buildList {
        addIfNotNull(visibilityModifier(symbol))
        if (symbol.isExpect) add(KtTokens.EXPECT_KEYWORD)
        if (symbol.isActual) add(KtTokens.ACTUAL_KEYWORD)
        if (symbol.isExternal) add(KtTokens.EXTERNAL_KEYWORD)
    }

    context(session: KaSession)
    private fun modalityModifier(symbol: KaDeclarationSymbol): KtModifierKeywordToken? {
        if (!shouldRenderModality(symbol)) return null

        return when (symbol.modality) {
            KaSymbolModality.ABSTRACT -> KtTokens.ABSTRACT_KEYWORD
            KaSymbolModality.OPEN -> KtTokens.OPEN_KEYWORD
            KaSymbolModality.SEALED -> KtTokens.SEALED_KEYWORD
            KaSymbolModality.FINAL -> null
        }
    }

    /**
     * Whether the modality of [symbol] is meaningful:
     *  - only plain classes carry an explicit modality; interfaces, objects, enums, and annotation classes have an implicit one;
     *  - interface members are implicitly `abstract`/`open`, so their modality is never rendered;
     *  - `open` is redundant when the containing class is `final` (a final class or object cannot be inherited from).
     */
    context(session: KaSession)
    private fun shouldRenderModality(symbol: KaDeclarationSymbol): Boolean {
        if (symbol is KaClassSymbol) {
            return symbol.classKind == KaClassKind.CLASS
        }

        if (symbol !is KaCallableSymbol) {
            return true
        }

        val containingClass = symbol.containingDeclaration as? KaClassSymbol
        if (containingClass?.classKind == KaClassKind.INTERFACE) {
            return false
        }

        if (symbol.modality == KaSymbolModality.OPEN && containingClass?.modality == KaSymbolModality.FINAL) {
            return false
        }

        return true
    }
}

context(session: KaSession)
internal fun visibilityModifier(symbol: KaDeclarationSymbol): KtModifierKeywordToken? {
    val visibility = symbol.visibility
    if (visibility == implicitVisibility(symbol)) return null

    return when (visibility) {
        KaSymbolVisibility.PRIVATE -> KtTokens.PRIVATE_KEYWORD
        KaSymbolVisibility.PROTECTED -> KtTokens.PROTECTED_KEYWORD
        KaSymbolVisibility.INTERNAL -> KtTokens.INTERNAL_KEYWORD
        else -> null
    }
}

/**
 * The visibility which [symbol] has when no visibility modifier is written in source code.
 *
 * The primary constructor of an enum class or an object is implicitly `private`, and the primary constructor of a sealed class is
 * implicitly `protected`. Every other declaration is implicitly `public`.
 */
context(session: KaSession)
private fun implicitVisibility(symbol: KaDeclarationSymbol): KaSymbolVisibility {
    if (symbol is KaConstructorSymbol && symbol.isPrimary) {
        val containingClass = symbol.containingDeclaration as? KaClassSymbol
        when {
            containingClass == null -> {}
            containingClass.classKind == KaClassKind.ENUM_CLASS || containingClass.classKind.isObject -> {
                return KaSymbolVisibility.PRIVATE
            }
            containingClass.modality == KaSymbolModality.SEALED -> {
                return KaSymbolVisibility.PROTECTED
            }
        }
    }

    return KaSymbolVisibility.PUBLIC
}
