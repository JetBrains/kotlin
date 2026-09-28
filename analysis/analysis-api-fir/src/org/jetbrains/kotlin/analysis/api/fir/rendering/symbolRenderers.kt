/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.rendering

import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.rendering.*
import org.jetbrains.kotlin.analysis.api.symbols.*
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.utils.getApiKClassOf
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.addRemoveModifier.sortModifiers
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addIfNotNull

internal fun KaRendererBuilder.pushSymbolRenderers() {
    push(SymbolRenderer)
    push(SymbolNameRenderer)
    push(SymbolModifiersRenderer)
    push(FileRenderer)
    push(DeclarationRenderer)
    pushEmpty(KaPiece.Script)
    push(DestructuringDeclarationRenderer)
    push(ClassInitializerRenderer)
    push(CallableRenderer)
    push(FunctionRenderer)
    push(VariableRenderer)
    push(ParameterRenderer)
    push(ClassifierRenderer)
    push(ClassRenderer)
}

private object SymbolRenderer : KaPieceRenderer<KaSymbol>(KaPiece.Symbol) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaSymbol, next: () -> Unit): Boolean {
        when (value) {
            is KaDeclarationSymbol -> render(value, KaPiece.Declaration)
            is KaPackageSymbol -> render(value, KaPiece.Package)
            is KaFileSymbol -> render(value, KaPiece.File)
            else -> renderFallbackSymbol(value)
        }
        return true
    }
}

private object FileRenderer : KaPieceRenderer<KaFileSymbol>(KaPiece.File) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaFileSymbol, next: () -> Unit): Boolean {
        render(value to AnnotationUseSiteTarget.FILE, KaPiece.Annotations)
        return true
    }
}

private object DeclarationRenderer : KaPieceRenderer<KaDeclarationSymbol>(KaPiece.Declaration) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaDeclarationSymbol, next: () -> Unit): Boolean {
        when (value) {
            is KaCallableSymbol -> render(value, KaPiece.Callable)
            is KaClassifierSymbol -> render(value, KaPiece.Classifier)
            is KaScriptSymbol -> render(value, KaPiece.Script)
            is KaDestructuringDeclarationSymbol -> render(value, KaPiece.DestructuringDeclaration)
            is KaClassInitializerSymbol -> render(value, KaPiece.ClassInitializer)
        }
        return true
    }
}

private object DestructuringDeclarationRenderer : KaPieceRenderer<KaDestructuringDeclarationSymbol>(KaPiece.DestructuringDeclaration) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaDestructuringDeclarationSymbol, next: () -> Unit): Boolean {
        val entries = value.entries

        // The declaration's own `val`/`var` keyword is not part of the symbol, so it is taken from the entries.
        output.keyword(if (entries.all { it.isVal }) KtTokens.VAL_KEYWORD else KtTokens.VAR_KEYWORD)

        output.group(KaPiece.SymbolName) {
            output.punctuation("(")
            entries.forEachIndexed { index, entry ->
                if (index > 0) {
                    output.punctuation(",")
                    output.space()
                }
                render(entry, KaPiece.SymbolName)
            }
            output.punctuation(")")
        }

        return true
    }
}

private object ClassInitializerRenderer : KaPieceRenderer<KaClassInitializerSymbol>(KaPiece.ClassInitializer) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaClassInitializerSymbol, next: () -> Unit): Boolean {
        output.keyword(KtTokens.INIT_KEYWORD, trailingSpace = false)
        return true
    }
}

private object CallableRenderer : KaPieceRenderer<KaCallableSymbol>(KaPiece.Callable) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaCallableSymbol, next: () -> Unit): Boolean {
        when (value) {
            is KaFunctionSymbol -> render(value, KaPiece.Function)
            is KaVariableSymbol -> render(value, KaPiece.Variable)
        }
        return true
    }
}

private object FunctionRenderer : KaPieceRenderer<KaFunctionSymbol>(KaPiece.Function) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaFunctionSymbol, next: () -> Unit): Boolean {
        when (value) {
            is KaNamedFunctionSymbol -> render(value, KaPiece.NamedFunction)
            is KaConstructorSymbol -> render(value, KaPiece.Constructor)
            is KaPropertyAccessorSymbol -> render(value, KaPiece.PropertyAccessor)
            is KaSamConstructorSymbol -> render(value, KaPiece.SamConstructor)
            is KaAnonymousFunctionSymbol -> render(value, KaPiece.AnonymousFunction)
        }
        return true
    }
}

private object VariableRenderer : KaPieceRenderer<KaVariableSymbol>(KaPiece.Variable) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaVariableSymbol, next: () -> Unit): Boolean {
        when (value) {
            is KaPropertySymbol -> render(value, KaPiece.Property)
            is KaLocalVariableSymbol -> render(value, KaPiece.LocalVariable)
            is KaJavaFieldSymbol -> render(value, KaPiece.JavaField)
            is KaBackingFieldSymbol -> render(value, KaPiece.BackingField)
            is KaEnumEntrySymbol -> render(value, KaPiece.EnumEntry)
            is KaParameterSymbol -> render(value, KaPiece.Parameter)
        }
        return true
    }
}

private object ParameterRenderer : KaPieceRenderer<KaParameterSymbol>(KaPiece.Parameter) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaParameterSymbol, next: () -> Unit): Boolean {
        when (value) {
            is KaValueParameterSymbol -> render(value, KaPiece.ValueParameter)
            is KaContextParameterSymbol -> render(value, KaPiece.ContextParameter)
            is KaReceiverParameterSymbol -> render(value, KaPiece.ReceiverParameter)
        }
        return true
    }
}

private object ClassifierRenderer : KaPieceRenderer<KaClassifierSymbol>(KaPiece.Classifier) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaClassifierSymbol, next: () -> Unit): Boolean {
        when (value) {
            is KaClassSymbol -> render(value, KaPiece.Class)
            is KaTypeAliasSymbol -> render(value, KaPiece.TypeAlias)
            is KaTypeParameterSymbol -> render(value, KaPiece.TypeParameter)
        }
        return true
    }
}

private object ClassRenderer : KaPieceRenderer<KaClassSymbol>(KaPiece.Class) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaClassSymbol, next: () -> Unit): Boolean {
        when (value) {
            is KaNamedClassSymbol -> render(value, KaPiece.NamedClass)
            is KaAnonymousObjectSymbol -> render(value, KaPiece.AnonymousObject)
        }
        return true
    }
}

/** Renders a symbol of an unknown kind, for which no dedicated piece exists. */
context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
private fun renderFallbackSymbol(value: KaSymbol) {
    if (value is KaNamedSymbol) {
        render(value, KaPiece.SymbolName)
    } else {
        val apiClass = getApiKClassOf(value)
        output.append("<${apiClass.qualifiedName}>", KaTextAttribute.Identifier)
    }
}

private object SymbolNameRenderer : KaPieceRenderer<KaNamedSymbol>(KaPiece.SymbolName) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaNamedSymbol, next: () -> Unit): Boolean {
        output.identifier(value.name, value)
        return true
    }
}

private object SymbolModifiersRenderer : KaPieceRenderer<KaDeclarationSymbol>(KaPiece.SymbolModifiers) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaDeclarationSymbol, next: () -> Unit): Boolean {
        val defaultModifiers = sortModifiers(defaultModifiers(value))
        val modifiers = context.valueFor(KaRenderingOption.Modifiers)(value, defaultModifiers)
        for (modifier in modifiers) {
            output.keyword(modifier)
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
            // A property accessor and a constructor carry no modality of their own; it always follows the containing declaration.
            is KaPropertyAccessorSymbol -> {
                addIfNotNull(visibilityModifier(symbol))
                if (symbol.isInline) add(KtTokens.INLINE_KEYWORD)
            }
            is KaConstructorSymbol -> {
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
