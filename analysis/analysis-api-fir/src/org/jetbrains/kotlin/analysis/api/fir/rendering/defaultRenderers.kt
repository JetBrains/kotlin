/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.rendering

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.base.KaContextReceiver
import org.jetbrains.kotlin.analysis.api.evaluation.evaluate
import org.jetbrains.kotlin.analysis.api.rendering.KaClassTypeQualification
import org.jetbrains.kotlin.analysis.api.rendering.KaClassTypeRenderingMode
import org.jetbrains.kotlin.analysis.api.rendering.KaParametrizedPieceRenderer
import org.jetbrains.kotlin.analysis.api.rendering.KaPiece
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderer
import org.jetbrains.kotlin.analysis.api.rendering.KaPieceRenderer
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOption
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOutput
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingContext
import org.jetbrains.kotlin.analysis.api.rendering.KaTextAttribute
import org.jetbrains.kotlin.analysis.api.rendering.KaTypeApproximation
import org.jetbrains.kotlin.analysis.api.rendering.append
import org.jetbrains.kotlin.analysis.api.rendering.pushEmpty
import org.jetbrains.kotlin.analysis.api.rendering.render
import org.jetbrains.kotlin.analysis.api.scopes.declaredMemberScope
import org.jetbrains.kotlin.analysis.api.symbols.KaAnonymousFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaAnonymousObjectSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaCallableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaClassKind
import org.jetbrains.kotlin.analysis.api.symbols.KaClassLikeSymbol
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
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySetterSymbol
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
import org.jetbrains.kotlin.analysis.api.symbols.containingModule
import org.jetbrains.kotlin.analysis.api.symbols.findClass
import org.jetbrains.kotlin.analysis.api.symbols.findPackage
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.types.KaCapturedType
import org.jetbrains.kotlin.analysis.api.types.KaClassErrorType
import org.jetbrains.kotlin.analysis.api.types.KaClassType
import org.jetbrains.kotlin.analysis.api.types.KaDefinitelyNotNullType
import org.jetbrains.kotlin.analysis.api.types.KaDynamicType
import org.jetbrains.kotlin.analysis.api.types.KaErrorType
import org.jetbrains.kotlin.analysis.api.types.KaFlexibleType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionType
import org.jetbrains.kotlin.analysis.api.types.KaFunctionTypeFamily
import org.jetbrains.kotlin.analysis.api.types.KaFunctionValueParameter
import org.jetbrains.kotlin.analysis.api.types.KaIntersectionType
import org.jetbrains.kotlin.analysis.api.types.KaResolvedClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.analysis.api.types.abbreviationOrSelf
import org.jetbrains.kotlin.analysis.api.types.approximateToDenotableSubtypeOrSelf
import org.jetbrains.kotlin.analysis.api.types.approximateToDenotableSupertypeOrSelf
import org.jetbrains.kotlin.analysis.api.types.classId
import org.jetbrains.kotlin.analysis.api.types.expandedSymbol
import org.jetbrains.kotlin.analysis.api.types.fullyExpandedType
import org.jetbrains.kotlin.analysis.api.types.functionTypeFamily
import org.jetbrains.kotlin.analysis.api.types.hasFlexibleNullability
import org.jetbrains.kotlin.analysis.api.types.isMarkedNullable
import org.jetbrains.kotlin.analysis.api.types.isNullable
import org.jetbrains.kotlin.analysis.api.types.symbol
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.lexer.KtKeywordToken
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.addRemoveModifier.sortModifiers
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.name.render
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addIfNotNull

internal val DEFAULT_RENDERER: KaRenderer = buildRenderer(null) {
    // Top-level dispatchers.
    push(SymbolRenderer)
    push(TypeRenderer)

    // Shared building blocks.
    push(SymbolNameRenderer)
    push(SymbolModifiersRenderer)
    push(AnnotationsRenderer)
    push(AnnotationRenderer)
    push(AnnotationValuesRenderer)
    push(AnnotationValueRenderer)
    push(ConstantValueRenderer)
    push(TypeAnnotationsRenderer)
    push(TypeNullabilityRenderer)

    // Types.
    push(ClassTypeRenderer)
    push(ClassTypeNameRenderer)
    push(FunctionTypeRenderer)
    push(FunctionTypeParameterRenderer)
    push(FunctionTypeContextParameterRenderer)
    push(TypeParameterTypeRenderer)
    push(CapturedTypeRenderer)
    push(DefinitelyNotNullTypeRenderer)
    push(FlexibleTypeRenderer)
    push(IntersectionTypeRenderer)
    push(DynamicTypeRenderer)
    push(ErrorTypeRenderer)
    push(TypeArgumentListRenderer)
    push(TypeProjectionRenderer)

    // Type parameters.
    push(TypeParameterListRenderer)
    push(CallableTypeParameterListRenderer)
    push(ClassifierTypeParameterListRenderer)
    push(TypeParameterRenderer)
    push(WhereClauseRenderer)
    push(CallableWhereClauseRenderer)
    push(ClassifierWhereClauseRenderer)

    // Context parameters.
    push(ContextParameterListRenderer)
    push(CallableContextParameterListRenderer)
    push(ContextParameterRenderer)
    push(ContextParameterAnnotationsRenderer)
    push(ContextParameterTypeRenderer)

    // Value parameters.
    push(ValueParameterListRenderer)
    push(ValueParameterRenderer)
    push(ValueParameterAnnotationsRenderer)
    push(ValueParameterTypeRenderer)
    push(ValueParameterDefaultValueRenderer)
    push(ValueParameterDefaultValueExpressionRenderer)

    // Functions.
    push(FunctionRenderer)
    push(FunctionAnnotationsRenderer)
    push(FunctionReceiverRenderer)
    push(FunctionValueParameterListRenderer)
    push(FunctionReturnTypeRenderer)
    push(NamedFunctionReturnTypeRenderer)
    push(NamedFunctionRenderer)
    push(ConstructorRenderer)
    push(PropertyAccessorRenderer)
    pushEmpty(KaPiece.FunctionBody)
    pushEmpty(KaPiece.ConstructorBody)

    // Properties and variables.
    push(PropertyRenderer)
    push(PropertyAnnotationsRenderer)
    push(PropertyReturnTypeRenderer)
    push(PropertyInitializerRenderer)
    push(PropertyAccessorsRenderer)
    push(LocalVariableRenderer)
    push(JavaFieldRenderer)
    push(BackingFieldRenderer)
    push(EnumEntryRenderer)

    // Classifiers.
    push(NamedClassRenderer)
    push(AnonymousObjectRenderer)
    push(TypeAliasRenderer)
    push(PrimaryConstructorRenderer)
    push(ClassAnnotationsRenderer)
    push(SupertypeListRenderer)
    push(SupertypeRenderer)
    push(ClassBodyRenderer)

    // Packages.
    push(PackageRenderer)
    push(PackageNameRenderer)
}

// -------------------------------------------------------------------------------------------------
// Shared helpers.
// -------------------------------------------------------------------------------------------------

/**
 * Renders [token] unless it is filtered out by [KaRenderingOption.AllowedKeywords].
 *
 * @param trailingSpace whether a space is appended after the keyword. Pass `false` when the keyword is directly followed by punctuation,
 * as in `constructor(`.
 */
context(context: KaRenderingContext, output: KaRenderingOutput)
private fun keyword(token: KtKeywordToken, trailingSpace: Boolean = true) {
    if (!context.valueFor(KaRenderingOption.AllowedKeywords)(token)) return

    output.append(token.value, KaTextAttribute.Keyword)
    if (trailingSpace) output.space()
}

/** Renders the `in`/`out` keyword of [variance]. Nothing is rendered for an invariant projection or type parameter. */
context(context: KaRenderingContext, output: KaRenderingOutput)
private fun varianceKeyword(variance: Variance) {
    when (variance) {
        Variance.IN_VARIANCE -> keyword(KtTokens.IN_KEYWORD)
        Variance.OUT_VARIANCE -> keyword(KtTokens.OUT_KEYWORD)
        Variance.INVARIANT -> {}
    }
}

/** The visibility modifier of [symbol], or `null` when the visibility is implicit and thus not written in source code. */
context(session: KaSession)
private fun visibilityModifier(symbol: KaDeclarationSymbol): KtModifierKeywordToken? {
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
            containingClass.classKind == KaClassKind.ENUM_CLASS || containingClass.classKind.isObject -> return KaSymbolVisibility.PRIVATE
            containingClass.modality == KaSymbolModality.SEALED -> return KaSymbolVisibility.PROTECTED
        }
    }

    return KaSymbolVisibility.PUBLIC
}

context(output: KaRenderingOutput)
private fun identifier(name: String, symbol: KaSymbol) {
    output.append(name, setOf(KaTextAttribute.Identifier, KaTextAttribute.Symbol(symbol)))
}

context(output: KaRenderingOutput)
private fun identifier(name: Name, symbol: KaSymbol) {
    // `Name.render()` wraps keywords and names with special characters (e.g. `<set-?>`) in backticks.
    identifier(name.render(), symbol)
}

/**
 * Renders annotations from [holder] on [owner] with the given [useSiteTarget].
 *
 * [holder] is a component of [owner] whose annotations cannot be rendered on their own declaration, such as the getter of a property
 * declared in a primary constructor. [owner] is the declaration they are rendered on, which decides their layout.
 */
context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
private fun renderAnnotations(owner: KaAnnotated, holder: KaAnnotated, useSiteTarget: AnnotationUseSiteTarget?) {
    var annotations = context.valueFor(KaRenderingOption.Annotations)(session, context, holder)
    if (holder !== owner) {
        // A component shares its annotations with the declaration it belongs to, so an annotation which is written without a use-site
        // target would otherwise be rendered twice: once for the declaration, and once for the component with a use-site target which is
        // not in the source code.
        annotations = annotations.filterNot { annotation -> isDeclaredOn(annotation, owner) }
    }

    if (annotations.isEmpty()) {
        return
    }

    val onNewLine = context.valueFor(KaRenderingOption.AnnotationsOnNewLine)(session, context, owner)
    for (annotation in annotations) {
        render(annotation, useSiteTarget, KaPiece.Annotation)
        if (onNewLine) output.newLine() else output.space()
    }
}

/** Whether [annotation] is written on [owner] itself, and so is already rendered as one of the owner's own annotations. */
private fun isDeclaredOn(annotation: KaAnnotation, owner: KaAnnotated): Boolean {
    val psi = annotation.psi ?: return false
    return owner.annotations.any { it.psi === psi }
}

/** Whether [holder] has any annotation which [renderAnnotations] would render on [owner]. */
context(session: KaSession, context: KaRenderingContext)
private fun hasRenderedAnnotations(owner: KaAnnotated, holder: KaAnnotated): Boolean {
    val annotations = context.valueFor(KaRenderingOption.Annotations)(session, context, holder)
    return if (holder === owner) {
        annotations.isNotEmpty()
    } else {
        annotations.any { annotation -> !isDeclaredOn(annotation, owner) }
    }
}

/**
 * Renders the qualified name of [type] as prescribed by [KaRenderingOption.ClassTypeQualification], including the type arguments of each
 * rendered qualifier segment. This is the implementation of [KaPiece.ClassTypeName].
 *
 * When [mutabilityFlexible] is `true`, every segment which names a mutable collection is rendered with the mutability-flexible marker
 * instead, e.g. `(Mutable)List` for `kotlin.collections.MutableList`, and `(Mutable)Map.(Mutable)Entry` for
 * `kotlin.collections.MutableMap.MutableEntry`. As the marker cannot be carried by [KaPiece.ClassTypeName], whose value is the class type
 * alone, [FlexibleTypeRenderer] calls this function directly instead of rendering that piece.
 */
context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
private fun renderClassTypeQualifiedName(type: KaClassType, mutabilityFlexible: Boolean = false) {
    val qualification = context.valueFor(KaRenderingOption.ClassTypeQualification)
    val qualifiers = type.qualifiers

    if (qualifiers.isEmpty()) {
        // Fallback for class types that expose no explicit qualifier segments.
        identifier(qualifierName(type.classId.shortClassName, mutabilityFlexible), type.symbol)
        if (type.typeArguments.isNotEmpty()) {
            render(type.typeArguments, KaPiece.TypeArgumentList)
        }
        return
    }

    if (qualification == KaClassTypeQualification.FULLY_QUALIFIED) {
        val packageFqName = type.classId.packageFqName
        if (!packageFqName.isRoot && packageFqName != CallableId.PACKAGE_FQ_NAME_FOR_LOCAL) {
            render(packageFqName, KaPiece.PackageName)
            output.append(".", KaTextAttribute.Punctuation)
        }
    }

    // `SIMPLE` keeps only the innermost segment; the other modes render all outer classifiers.
    val renderedQualifiers = when (qualification) {
        KaClassTypeQualification.SIMPLE -> listOf(qualifiers.last())
        else -> qualifiers
    }

    renderedQualifiers.forEachIndexed { index, qualifier ->
        if (index > 0) output.append(".", KaTextAttribute.Punctuation)
        identifier(qualifierName(qualifier.name, mutabilityFlexible), qualifier.symbol)
        if (qualifier.typeArguments.isNotEmpty()) {
            render(qualifier.typeArguments, KaPiece.TypeArgumentList)
        }
    }
}

/** The rendered name of a single qualifier segment, e.g. `List`, or `(Mutable)List` for a [mutabilityFlexible] `MutableList`. */
private fun qualifierName(name: Name, mutabilityFlexible: Boolean): String {
    // `Name.render()` wraps keywords and names with special characters in backticks.
    val rendered = name.render()
    if (!mutabilityFlexible || !rendered.startsWith(MUTABLE_NAME_PREFIX)) return rendered
    return "($MUTABLE_NAME_PREFIX)" + rendered.removePrefix(MUTABLE_NAME_PREFIX)
}

/** The name prefix shared by all mutable collections in [StandardClassIds.Collections.mutableCollectionToBaseCollection]. */
private const val MUTABLE_NAME_PREFIX = "Mutable"

/**
 * Whether [this] type is rendered with function type syntax, such as `(Int) -> String`.
 *
 * [Reflection function types][KaFunctionType.isReflectType], such as `KFunction1<Int, String>`, are function types in the type system, but
 * they can only be written as class types. Rendering them with function type syntax would drop the very part which distinguishes them from
 * a plain `Function1<Int, String>`.
 */
private val KaType.isRenderedAsFunctionType: Boolean
    get() = this is KaFunctionType && !isReflectType

/**
 * Renders a receiver type of extension callable or a function type, wrapping it in parentheses when required by the Kotlin grammar
 * (function types rendered with function type syntax, and definitely non-nullable types).
 *
 * A nullable type needs no parentheses, as the Kotlin grammar allows it as a receiver type on its own, e.g. `fun String?.foo()`.
 */
context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
private fun renderReceiverType(type: KaType) {
    val parenthesized = type.isRenderedAsFunctionType || type is KaDefinitelyNotNullType
    if (parenthesized) output.append("(", KaTextAttribute.GroupStart)
    render(type, KaPiece.Type)
    if (parenthesized) output.append(")", KaTextAttribute.GroupEnd)
}

/**
 * Renders the annotations of [type] as [KaPiece.TypeAnnotations].
 *
 * [KaPiece.Type] only dispatches to the piece of the type's kind, so each of those pieces renders the annotations of its own type. A
 * composite type shares its annotations with the components it is built from: a flexible type carries those of its lower bound, a
 * definitely non-nullable type those of its original type, and an intersection type those which all of its conjuncts have in common.
 * A piece which renders such a component as a type therefore leaves the annotations to it, as rendering them here as well would print the
 * same annotation twice.
 */
context(session: KaSession, context: KaRenderingContext)
private fun renderTypeAnnotations(type: KaType) {
    if (type.annotations.isEmpty()) return
    render(type, KaPiece.TypeAnnotations)
}

// -------------------------------------------------------------------------------------------------
// Symbols: top-level dispatcher.
// -------------------------------------------------------------------------------------------------

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

/**
 * Renders all modifiers of a declaration, e.g. `private abstract`.
 *
 * The default modifiers of the declaration are collected, sorted into the canonical Kotlin order, and then passed through
 * [KaRenderingOption.Modifiers], which may drop, reorder, or add modifiers. The resulting modifiers are rendered in their resulting order.
 */
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
                // The `fun` of a `fun interface` is a modifier, unlike the `fun` which introduces a function declaration.
                if (symbol.isFun) add(KtTokens.FUN_KEYWORD)
            }
            is KaTypeAliasSymbol -> {
                addAll(commonModifiers(symbol))
            }
            // The `static` of a Java field has no Kotlin modifier keyword, so it is rendered by `JavaFieldRenderer` instead.
            is KaJavaFieldSymbol -> {
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
            // Enum entries, backing fields, and context parameters cannot carry modifiers.
            else -> {}
        }
    }

    /** The modifiers which every declaration may carry. */
    context(session: KaSession)
    private fun commonModifiers(symbol: KaDeclarationSymbol): List<KtModifierKeywordToken> = buildList {
        addIfNotNull(visibilityModifier(symbol))
        if (symbol.isExpect) add(KtTokens.EXPECT_KEYWORD)
        if (symbol.isActual) add(KtTokens.ACTUAL_KEYWORD)
        if (symbol.isExternal) add(KtTokens.EXTERNAL_KEYWORD)
    }

    /** The modality modifier of [symbol], or `null` when the modality is final, implicit, or redundant. */
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

        if (symbol !is KaCallableSymbol) return true

        val containingClass = symbol.containingDeclaration as? KaClassSymbol
        if (containingClass?.classKind == KaClassKind.INTERFACE) return false
        if (symbol.modality == KaSymbolModality.OPEN && containingClass?.modality == KaSymbolModality.FINAL) return false

        return true
    }
}

private object AnnotationsRenderer : KaParametrizedPieceRenderer<KaAnnotated, AnnotationUseSiteTarget?>(KaPiece.Annotations) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaAnnotated, next: () -> Unit): Boolean {
        return render(value, data = null, next)
    }

    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaAnnotated, data: AnnotationUseSiteTarget?, next: () -> Unit): Boolean {
        renderAnnotations(owner = value, holder = value, useSiteTarget = data)
        return true
    }
}

private object AnnotationRenderer : KaParametrizedPieceRenderer<KaAnnotation, AnnotationUseSiteTarget?>(KaPiece.Annotation) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaAnnotation, next: () -> Unit): Boolean {
        return render(value, data = null, next)
    }

    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaAnnotation, data: AnnotationUseSiteTarget?, next: () -> Unit): Boolean {
        output.append("@", KaTextAttribute.Punctuation)
        if (data != null) {
            output.append(data.renderName, KaTextAttribute.Keyword)
            output.append(":", KaTextAttribute.Punctuation)
        }
        renderAnnotationName(value.classId)
        render(value, KaPiece.AnnotationValues)
        return true
    }

    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    private fun renderAnnotationName(classId: ClassId?) {
        if (classId == null) {
            output.append("ERROR", KaTextAttribute.Identifier)
            return
        }

        val qualification = context.valueFor(KaRenderingOption.ClassTypeQualification)

        if (qualification == KaClassTypeQualification.FULLY_QUALIFIED) {
            val packageFqName = classId.packageFqName
            if (!packageFqName.isRoot && packageFqName != CallableId.PACKAGE_FQ_NAME_FOR_LOCAL) {
                render(packageFqName, KaPiece.PackageName)
                output.append(".", KaTextAttribute.Punctuation)
            }
        }

        // The class ID which each segment names, so that every segment can be linked to the classifier it refers to.
        val segments = buildList {
            var segmentClassId: ClassId? = null
            for (segmentName in classId.relativeClassName.pathSegments()) {
                segmentClassId = segmentClassId?.createNestedClassId(segmentName) ?: ClassId(classId.packageFqName, segmentName)
                add(segmentName to segmentClassId)
            }
        }

        // `SIMPLE` keeps only the innermost segment; the other modes render all outer classifiers.
        val renderedSegments = when (qualification) {
            KaClassTypeQualification.SIMPLE -> listOf(segments.last())
            else -> segments
        }

        renderedSegments.forEachIndexed { index, [segmentName, segmentClassId] ->
            if (index > 0) output.append(".", KaTextAttribute.Punctuation)
            val symbol = findClass(segmentClassId)
            if (symbol != null) {
                identifier(segmentName, symbol)
            } else {
                output.append(segmentName.render(), KaTextAttribute.Identifier)
            }
        }
    }
}

private object AnnotationValuesRenderer : KaPieceRenderer<KaAnnotation>(KaPiece.AnnotationValues) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaAnnotation, next: () -> Unit): Boolean {
        val arguments = value.arguments
        if (arguments.isEmpty()) return true

        output.append("(", KaTextAttribute.GroupStart)
        arguments.forEachIndexed { index, argument ->
            if (index > 0) output.append(", ", KaTextAttribute.Punctuation)
            output.append(argument.name.asString(), KaTextAttribute.Identifier)
            output.append(" = ", KaTextAttribute.Punctuation)
            render(argument.expression, KaPiece.AnnotationValue)
        }
        output.append(")", KaTextAttribute.GroupEnd)
        return true
    }
}

private object AnnotationValueRenderer : KaPieceRenderer<KaAnnotationValue>(KaPiece.AnnotationValue) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaAnnotationValue, next: () -> Unit): Boolean {
        when (value) {
            is KaAnnotationValue.ConstantValue -> render(value.value, KaPiece.ConstantValue)
            is KaAnnotationValue.EnumEntryValue -> {
                val callableId = value.callableId
                if (callableId != null) {
                    callableId.className?.let { className ->
                        output.append(className.shortName().asString(), KaTextAttribute.Identifier)
                        output.append(".", KaTextAttribute.Punctuation)
                    }
                    output.append(callableId.callableName.asString(), KaTextAttribute.Identifier)
                } else {
                    output.append("ERROR", KaTextAttribute.Identifier)
                }
            }
            is KaAnnotationValue.ClassLiteralValue -> {
                render(value.type, KaPiece.Type)
                output.append("::", KaTextAttribute.Punctuation)
                output.append("class", KaTextAttribute.Keyword)
            }
            is KaAnnotationValue.NestedAnnotationValue -> {
                render(value.annotation, KaPiece.Annotation)
            }
            is KaAnnotationValue.ArrayValue -> {
                output.append("[", KaTextAttribute.GroupStart)
                value.values.forEachIndexed { index, element ->
                    if (index > 0) output.append(", ", KaTextAttribute.Punctuation)
                    render(element, KaPiece.AnnotationValue)
                }
                output.append("]", KaTextAttribute.GroupEnd)
            }
            is KaAnnotationValue.UnsupportedValue -> {
                output.append("ERROR", KaTextAttribute.Identifier)
            }
        }
        return true
    }
}

private object ConstantValueRenderer : KaPieceRenderer<KaConstantValue>(KaPiece.ConstantValue) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaConstantValue, next: () -> Unit): Boolean {
        when (value) {
            is KaConstantValue.NullValue, is KaConstantValue.BooleanValue -> {
                output.append(value.render(), KaTextAttribute.Keyword)
            }
            is KaConstantValue.StringValue, is KaConstantValue.CharValue -> {
                output.append(value.render(), KaTextAttribute.StringLiteral)
            }
            is KaConstantValue.ErrorValue -> {
                output.append("ERROR", KaTextAttribute.Identifier)
            }
            else -> {
                output.append(value.render(), KaTextAttribute.NumberLiteral)
            }
        }
        return true
    }
}

// -------------------------------------------------------------------------------------------------
// Functions.
// -------------------------------------------------------------------------------------------------

private object NamedFunctionRenderer : KaPieceRenderer<KaNamedFunctionSymbol>(KaPiece.NamedFunction) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaNamedFunctionSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.FunctionAnnotations)
        render(value, KaPiece.CallableContextParameterList)
        render(value, KaPiece.SymbolModifiers)
        keyword(KtTokens.FUN_KEYWORD)

        if (value.typeParameters.isNotEmpty()) {
            render(value, KaPiece.CallableTypeParameterList)
            output.space()
        }

        value.receiverParameter?.let { render(it, KaPiece.FunctionReceiver) }
        render(value, KaPiece.SymbolName)
        render(value, KaPiece.FunctionValueParameterList)
        render(value, KaPiece.NamedFunctionReturnType)
        render(value, KaPiece.CallableWhereClause)
        render(value, KaPiece.FunctionBody)

        return true
    }
}

/** Generic function renderer used for anonymous functions and SAM constructors. */
private object FunctionRenderer : KaPieceRenderer<KaFunctionSymbol>(KaPiece.Function) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaFunctionSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.FunctionAnnotations)
        render(value, KaPiece.SymbolModifiers)
        keyword(KtTokens.FUN_KEYWORD)

        value.receiverParameter?.let { render(it, KaPiece.FunctionReceiver) }

        if (value is KaNamedSymbol) {
            render(value, KaPiece.SymbolName)
        }

        render(value, KaPiece.FunctionValueParameterList)
        render(value, KaPiece.FunctionReturnType)
        render(value, KaPiece.FunctionBody)

        return true
    }
}

private object ConstructorRenderer : KaPieceRenderer<KaConstructorSymbol>(KaPiece.Constructor) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaConstructorSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.Annotations)
        render(value, KaPiece.SymbolModifiers)
        keyword(KtTokens.CONSTRUCTOR_KEYWORD, trailingSpace = false)
        render(value, KaPiece.FunctionValueParameterList)
        render(value, KaPiece.ConstructorBody)

        return true
    }
}

private object PrimaryConstructorRenderer : KaPieceRenderer<KaConstructorSymbol>(KaPiece.PrimaryConstructor) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaConstructorSymbol, next: () -> Unit): Boolean {
        // The `constructor` keyword is rendered when the primary constructor carries its own annotations, or a visibility which is
        // written in source code.
        if (value.annotations.isNotEmpty() || visibilityModifier(value) != null) {
            output.space()
            render(value, KaPiece.Annotations)
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

private object PropertyAccessorRenderer : KaPieceRenderer<KaPropertyAccessorSymbol>(KaPiece.PropertyAccessor) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaPropertyAccessorSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.Annotations)
        render(value, KaPiece.SymbolModifiers)
        val isGetter = value is KaPropertyGetterSymbol
        keyword(if (isGetter) KtTokens.GET_KEYWORD else KtTokens.SET_KEYWORD, trailingSpace = false)
        // A default accessor has no parameter list in source code, as in `@Anno get` or `private set`.
        if (value.isNotDefault) {
            output.append("(", KaTextAttribute.GroupStart)
            if (value is KaPropertySetterSymbol) {
                render(value.parameter, KaPiece.ValueParameter)
            }
            output.append(")", KaTextAttribute.GroupEnd)
        }
        render(value, KaPiece.FunctionBody)
        return true
    }
}

private object FunctionAnnotationsRenderer : KaPieceRenderer<KaFunctionSymbol>(KaPiece.FunctionAnnotations) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaFunctionSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.Annotations)
        return true
    }
}

private object FunctionReceiverRenderer : KaPieceRenderer<KaParameterSymbol>(KaPiece.FunctionReceiver) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaParameterSymbol, next: () -> Unit): Boolean {
        // A receiver parameter has no declaration of its own, so its annotations can only be rendered with a use-site target.
        render(value, AnnotationUseSiteTarget.RECEIVER, KaPiece.Annotations)
        renderReceiverType(value.returnType)
        output.append(".", KaTextAttribute.Punctuation)
        return true
    }
}


private object FunctionValueParameterListRenderer : KaPieceRenderer<KaFunctionSymbol>(KaPiece.FunctionValueParameterList) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaFunctionSymbol, next: () -> Unit): Boolean {
        render(value.valueParameters, KaPiece.ValueParameterList)
        return true
    }
}

private object FunctionReturnTypeRenderer : KaPieceRenderer<KaFunctionSymbol>(KaPiece.FunctionReturnType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaFunctionSymbol, next: () -> Unit): Boolean {
        output.append(": ", KaTextAttribute.Punctuation)
        render(value.returnType, KaPiece.Type)
        return true
    }
}

private object NamedFunctionReturnTypeRenderer : KaPieceRenderer<KaNamedFunctionSymbol>(KaPiece.NamedFunctionReturnType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaNamedFunctionSymbol, next: () -> Unit): Boolean {
        // An implicit `Unit` return type is omitted in source code. A nullable or annotated `Unit` is not implicit, so it is rendered.
        val returnType = value.returnType
        val isImplicitUnit = returnType.classId == StandardClassIds.Unit &&
                !returnType.isNullable &&
                returnType.annotations.isEmpty()

        if (!isImplicitUnit) {
            render(value, KaPiece.FunctionReturnType)
        }
        return true
    }
}

// -------------------------------------------------------------------------------------------------
// Value parameters.
// -------------------------------------------------------------------------------------------------

private object ValueParameterListRenderer : KaPieceRenderer<List<KaValueParameterSymbol>>(KaPiece.ValueParameterList) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: List<KaValueParameterSymbol>, next: () -> Unit): Boolean {
        output.append("(", KaTextAttribute.GroupStart)
        value.forEachIndexed { index, parameter ->
            if (index > 0) output.append(", ", KaTextAttribute.Punctuation)
            render(parameter, KaPiece.ValueParameter)
        }
        output.append(")", KaTextAttribute.GroupEnd)
        return true
    }
}

private object ValueParameterRenderer : KaPieceRenderer<KaValueParameterSymbol>(KaPiece.ValueParameter) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaValueParameterSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.ValueParameterAnnotations)

        // When the primary constructor is rendered in the class header, the parameter is the declaration of the property, so it carries
        // the property's modifiers and its `val`/`var`. Otherwise, the property is rendered as a class member instead.
        val property = declaredProperty(value)
        if (property != null) {
            render(property, KaPiece.SymbolModifiers)
        }
        render(value, KaPiece.SymbolModifiers)
        if (property != null) {
            keyword(if (property.isVal) KtTokens.VAL_KEYWORD else KtTokens.VAR_KEYWORD)
        }

        render(value, KaPiece.SymbolName)
        render(value, KaPiece.ValueParameterType)
        render(value, KaPiece.ValueParameterDefaultValue)
        return true
    }
}

private object ValueParameterAnnotationsRenderer : KaPieceRenderer<KaValueParameterSymbol>(KaPiece.ValueParameterAnnotations) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaValueParameterSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.Annotations)

        // A value parameter list has no room for accessors, so every component of a property declared in the primary constructor has its
        // annotations rendered on the parameter, with a use-site target.
        declaredProperty(value)?.let { correspondingProperty ->
            renderAnnotations(owner = value, holder = correspondingProperty, useSiteTarget = AnnotationUseSiteTarget.PROPERTY)
            correspondingProperty.backingFieldSymbol?.let { backingField ->
                renderAnnotations(owner = value, holder = backingField, useSiteTarget = AnnotationUseSiteTarget.FIELD)
            }
            correspondingProperty.getter?.let { getter ->
                renderAnnotations(owner = value, holder = getter, useSiteTarget = AnnotationUseSiteTarget.PROPERTY_GETTER)
            }
            correspondingProperty.setter?.let { setter ->
                renderAnnotations(owner = value, holder = setter, useSiteTarget = AnnotationUseSiteTarget.PROPERTY_SETTER)
                renderAnnotations(owner = value, holder = setter.parameter, useSiteTarget = AnnotationUseSiteTarget.SETTER_PARAMETER)
            }
        }

        return true
    }
}

/**
 * The property which [parameter] declares in a primary constructor, provided the parameter is the property's rendered declaration.
 * Returns `null` for any other value parameter, and when the property is rendered as a class member instead.
 */
context(context: KaRenderingContext)
private fun declaredProperty(parameter: KaValueParameterSymbol): KaKotlinPropertySymbol? {
    if (!context.valueFor(KaRenderingOption.PrimaryConstructorInClassHeader)) return null
    return parameter.primaryConstructorProperty
}

private object ValueParameterTypeRenderer : KaPieceRenderer<KaValueParameterSymbol>(KaPiece.ValueParameterType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaValueParameterSymbol, next: () -> Unit): Boolean {
        output.append(": ", KaTextAttribute.Punctuation)
        render(value.returnType, KaPiece.Type)
        return true
    }
}

private object ValueParameterDefaultValueRenderer : KaPieceRenderer<KaValueParameterSymbol>(KaPiece.ValueParameterDefaultValue) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaValueParameterSymbol, next: () -> Unit): Boolean {
        if (!value.hasDefaultValue) return true

        output.append(" = ", KaTextAttribute.Punctuation)
        render(value, KaPiece.ValueParameterDefaultValueExpression)
        return true
    }
}

private object ValueParameterDefaultValueExpressionRenderer :
    KaPieceRenderer<KaValueParameterSymbol>(KaPiece.ValueParameterDefaultValueExpression) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaValueParameterSymbol, next: () -> Unit): Boolean {
        val constant = constantDefaultValue(value)
        if (constant != null) {
            render(constant, KaPiece.ConstantValue)
        } else {
            // The expression is not part of the symbol, so an expression which is not a compile-time constant is rendered as a placeholder.
            output.append("...", KaTextAttribute.Punctuation)
        }
        return true
    }

    /**
     * The compile-time constant which [value] defaults to, or `null` if the default value is absent or is not a constant.
     *
     * The default value expression is not part of the symbol, so it has to be evaluated from source code, which is only possible for a
     * parameter of the module under analysis.
     */
    context(session: KaSession)
    private fun constantDefaultValue(value: KaValueParameterSymbol): KaConstantValue? {
        if (value.containingModule != session.useSiteModule) return null

        val defaultValueExpression = (value.realPsi as? KtParameter)?.defaultValue ?: return null
        return defaultValueExpression.evaluate()?.takeUnless { it is KaConstantValue.ErrorValue }
    }
}

// -------------------------------------------------------------------------------------------------
// Context parameters.
// -------------------------------------------------------------------------------------------------

private object ContextParameterListRenderer : KaPieceRenderer<List<KaContextParameterSymbol>>(KaPiece.ContextParameterList) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: List<KaContextParameterSymbol>, next: () -> Unit): Boolean {
        keyword(KtTokens.CONTEXT_KEYWORD, trailingSpace = false)
        output.append("(", KaTextAttribute.GroupStart)
        value.forEachIndexed { index, parameter ->
            if (index > 0) output.append(", ", KaTextAttribute.Punctuation)
            render(parameter, KaPiece.ContextParameter)
        }
        output.append(")", KaTextAttribute.GroupEnd)
        val onNewLine = context.valueFor(KaRenderingOption.ContextReceiversOnNewLine)(session, context, value)
        if (onNewLine) output.newLine() else output.space()
        return true
    }
}

private object CallableContextParameterListRenderer : KaPieceRenderer<KaCallableSymbol>(KaPiece.CallableContextParameterList) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaCallableSymbol, next: () -> Unit): Boolean {
        val contextParameters = value.contextParameters
        if (contextParameters.isNotEmpty()) {
            render(contextParameters, KaPiece.ContextParameterList)
        }
        return true
    }
}

private object ContextParameterRenderer : KaPieceRenderer<KaContextParameterSymbol>(KaPiece.ContextParameter) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaContextParameterSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.ContextParameterAnnotations)
        render(value, KaPiece.SymbolModifiers)
        render(value, KaPiece.SymbolName)
        render(value, KaPiece.ContextParameterType)
        return true
    }
}

private object ContextParameterAnnotationsRenderer : KaPieceRenderer<KaContextParameterSymbol>(KaPiece.ContextParameterAnnotations) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaContextParameterSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.Annotations)
        return true
    }
}

private object ContextParameterTypeRenderer : KaPieceRenderer<KaContextParameterSymbol>(KaPiece.ContextParameterType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaContextParameterSymbol, next: () -> Unit): Boolean {
        output.append(": ", KaTextAttribute.Punctuation)
        render(value.returnType, KaPiece.Type)
        return true
    }
}

// -------------------------------------------------------------------------------------------------
// Properties and variables.
// -------------------------------------------------------------------------------------------------

private object PropertyRenderer : KaPieceRenderer<KaPropertySymbol>(KaPiece.Property) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaPropertySymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.PropertyAnnotations)
        render(value, KaPiece.CallableContextParameterList)
        render(value, KaPiece.SymbolModifiers)
        keyword(if (value.isVal) KtTokens.VAL_KEYWORD else KtTokens.VAR_KEYWORD)

        if (value.typeParameters.isNotEmpty()) {
            render(value, KaPiece.CallableTypeParameterList)
            output.space()
        }

        value.receiverParameter?.let { render(it, KaPiece.FunctionReceiver) }

        render(value, KaPiece.SymbolName)
        render(value, KaPiece.PropertyReturnType)

        render(value, KaPiece.CallableWhereClause)

        render(value, KaPiece.PropertyInitializer)

        render(value, KaPiece.PropertyAccessors)
        return true
    }
}

private object PropertyAccessorsRenderer : KaPieceRenderer<KaPropertySymbol>(KaPiece.PropertyAccessors) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaPropertySymbol, next: () -> Unit): Boolean {
        val getter = value.getter?.takeIf { isAccessorRendered(it) }
        val setter = value.setter?.takeIf { isAccessorRendered(it) }
        if (getter == null && setter == null) return true

        output.indent()
        getter?.let { output.newLine(); render(it, KaPiece.PropertyAccessor) }
        setter?.let { output.newLine(); render(it, KaPiece.PropertyAccessor) }
        output.unindent()
        return true
    }
}

private object PropertyAnnotationsRenderer : KaPieceRenderer<KaPropertySymbol>(KaPiece.PropertyAnnotations) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaPropertySymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.Annotations)

        // The annotations of the accessors are rendered on the accessors themselves, but a backing field has no declaration of its own,
        // so its annotations can only be rendered with a use-site target.
        value.backingFieldSymbol?.let { backingField ->
            val useSiteTarget = if (value.isDelegated) {
                AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD
            } else {
                AnnotationUseSiteTarget.FIELD
            }
            renderAnnotations(owner = value, holder = backingField, useSiteTarget = useSiteTarget)
        }

        // A default setter is rendered without a parameter list, so the annotations of its parameter need a use-site target as well.
        value.setter?.takeIf { !it.isNotDefault }?.let { setter ->
            renderAnnotations(owner = value, holder = setter.parameter, useSiteTarget = AnnotationUseSiteTarget.SETTER_PARAMETER)
        }

        return true
    }
}

/**
 * Whether [accessor] is rendered below the property it belongs to. A default accessor is rendered when it carries annotations, as those
 * cannot be rendered anywhere else.
 */
context(session: KaSession, context: KaRenderingContext)
private fun isAccessorRendered(accessor: KaPropertyAccessorSymbol): Boolean {
    return accessor.isNotDefault || hasRenderedAnnotations(owner = accessor, holder = accessor)
}

private object PropertyReturnTypeRenderer : KaPieceRenderer<KaPropertySymbol>(KaPiece.PropertyReturnType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaPropertySymbol, next: () -> Unit): Boolean {
        output.append(": ", KaTextAttribute.Punctuation)
        render(value.returnType, KaPiece.Type)
        return true
    }
}

private object PropertyInitializerRenderer : KaPieceRenderer<KaPropertySymbol>(KaPiece.PropertyInitializer) {
    @OptIn(KaExperimentalApi::class)
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaPropertySymbol, next: () -> Unit): Boolean {
        // Only the initializer of a `const` property is rendered, as it is the only one which is part of the property's declaration in
        // source code and is always a compile-time constant.
        if (value !is KaKotlinPropertySymbol || !value.isConst) return true

        val constant = (value.initializer as? KaConstantInitializerValue)?.constant ?: return true
        output.append(" = ", KaTextAttribute.Punctuation)
        render(constant, KaPiece.ConstantValue)
        return true
    }
}

private object LocalVariableRenderer : KaPieceRenderer<KaLocalVariableSymbol>(KaPiece.LocalVariable) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaLocalVariableSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.Annotations)
        render(value, KaPiece.SymbolModifiers)
        keyword(if (value.isVal) KtTokens.VAL_KEYWORD else KtTokens.VAR_KEYWORD)
        render(value, KaPiece.SymbolName)
        output.append(": ", KaTextAttribute.Punctuation)
        render(value.returnType, KaPiece.Type)
        return true
    }
}

private object JavaFieldRenderer : KaPieceRenderer<KaJavaFieldSymbol>(KaPiece.JavaField) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaJavaFieldSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.Annotations)
        render(value, KaPiece.SymbolModifiers)
        // `static` is a Java modifier and has no Kotlin keyword token, so it is not covered by `AllowedKeywords` or `Modifiers`.
        if (value.isStatic) output.append("static", KaTextAttribute.Keyword).space()
        keyword(if (value.isVal) KtTokens.VAL_KEYWORD else KtTokens.VAR_KEYWORD)
        render(value, KaPiece.SymbolName)
        output.append(": ", KaTextAttribute.Punctuation)
        render(value.returnType, KaPiece.Type)
        return true
    }
}

private object BackingFieldRenderer : KaPieceRenderer<KaBackingFieldSymbol>(KaPiece.BackingField) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaBackingFieldSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.Annotations)
        render(value, KaPiece.SymbolModifiers)
        keyword(KtTokens.FIELD_KEYWORD, trailingSpace = false)
        output.append(": ", KaTextAttribute.Punctuation)
        render(value.returnType, KaPiece.Type)
        return true
    }
}

private object EnumEntryRenderer : KaPieceRenderer<KaEnumEntrySymbol>(KaPiece.EnumEntry) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaEnumEntrySymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.Annotations)
        render(value, KaPiece.SymbolModifiers)
        render(value, KaPiece.SymbolName)
        // Render the anonymous object body of the entry, e.g. `ENTRY { override fun foo() }`.
        value.initializer?.let { initializer ->
            render(initializer, KaPiece.ClassBody)
        }
        return true
    }
}

// -------------------------------------------------------------------------------------------------
// Classifiers.
// -------------------------------------------------------------------------------------------------

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
        render(value, KaPiece.Annotations)
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
        render(value, KaPiece.Annotations)
        return true
    }
}


private object SupertypeListRenderer : KaPieceRenderer<KaClassSymbol>(KaPiece.SupertypeList) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaClassSymbol, next: () -> Unit): Boolean {
        val superTypes = value.superTypes.filter { !it.isTrivialSuperType() }
        if (superTypes.isNotEmpty()) {
            output.append(" : ", KaTextAttribute.Punctuation)
            superTypes.forEachIndexed { index, superType ->
                if (index > 0) output.append(", ", KaTextAttribute.Punctuation)
                render(superType, KaPiece.Supertype)
            }
        }
        return true
    }

    /** `kotlin.Any` and the implicit `kotlin.Enum`/`kotlin.Annotation` supertypes are omitted from super type lists. */
    context(session: KaSession)
    private fun KaType.isTrivialSuperType(): Boolean {
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
            output.append("(", KaTextAttribute.GroupStart)
            output.append(")", KaTextAttribute.GroupEnd)
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

        val extraLineBetweenMembers = context.valueFor(KaRenderingOption.ExtraLineBetweenMembers)

        output.space().append("{", KaTextAttribute.GroupStart).indent()

        val enumEntries = members.filterIsInstance<KaEnumEntrySymbol>()
        val otherMembers = members.filter { it !is KaEnumEntrySymbol }

        // Enum entries come first, separated by commas, and are terminated with a semicolon when other declarations follow.
        enumEntries.forEachIndexed { index, entry ->
            output.newLine()
            render(entry, KaPiece.Symbol)
            when {
                index < enumEntries.lastIndex -> output.append(",", KaTextAttribute.Punctuation)
                otherMembers.isNotEmpty() -> output.append(";", KaTextAttribute.Punctuation)
            }
        }

        otherMembers.forEachIndexed { index, member ->
            // Separate members (and the enum entry block from the following declarations) with a blank line when requested.
            if (extraLineBetweenMembers && (enumEntries.isNotEmpty() || index > 0)) {
                output.newLine()
            }
            output.newLine()
            render(member, KaPiece.Symbol)
        }

        output.unindent().newLine().append("}", KaTextAttribute.GroupEnd)
        return true
    }
}

private object PackageRenderer : KaPieceRenderer<KaPackageSymbol>(KaPiece.Package) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaPackageSymbol, next: () -> Unit): Boolean {
        keyword(KtTokens.PACKAGE_KEYWORD)
        render(value, KaPiece.SymbolName)
        return true
    }
}

/** Renders a package name segment by segment, linking each segment to the package it forms. */
private object PackageNameRenderer : KaPieceRenderer<FqName>(KaPiece.PackageName) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: FqName, next: () -> Unit): Boolean {
        var current = FqName.ROOT
        value.pathSegments().forEachIndexed { index, segment ->
            if (index > 0) output.append(".", KaTextAttribute.Punctuation)
            current = current.child(segment)
            val packageSymbol = findPackage(current)
            if (packageSymbol != null) {
                identifier(segment, packageSymbol)
            } else {
                output.append(segment.render(), KaTextAttribute.Identifier)
            }
        }
        return true
    }
}

// -------------------------------------------------------------------------------------------------
// Type parameters.
// -------------------------------------------------------------------------------------------------

private object TypeParameterListRenderer : KaPieceRenderer<List<KaTypeParameterSymbol>>(KaPiece.TypeParameterList) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: List<KaTypeParameterSymbol>, next: () -> Unit): Boolean {
        output.append("<", KaTextAttribute.GroupStart)
        value.forEachIndexed { index, typeParameter ->
            if (index > 0) output.append(", ", KaTextAttribute.Punctuation)
            render(typeParameter, KaPiece.TypeParameter)
        }
        output.append(">", KaTextAttribute.GroupEnd)
        return true
    }
}

private object CallableTypeParameterListRenderer : KaPieceRenderer<KaCallableSymbol>(KaPiece.CallableTypeParameterList) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaCallableSymbol, next: () -> Unit): Boolean {
        render(value.typeParameters, KaPiece.TypeParameterList)
        return true
    }
}

private object ClassifierTypeParameterListRenderer : KaPieceRenderer<KaClassLikeSymbol>(KaPiece.ClassifierTypeParameterList) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaClassLikeSymbol, next: () -> Unit): Boolean {
        render(value.typeParameters, KaPiece.TypeParameterList)
        return true
    }
}

private object TypeParameterRenderer : KaPieceRenderer<KaTypeParameterSymbol>(KaPiece.TypeParameter) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaTypeParameterSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.Annotations)
        render(value, KaPiece.SymbolModifiers)

        render(value, KaPiece.SymbolName)

        // A single upper bound is rendered inline; multiple bounds are deferred to a `where` clause.
        if (value.upperBounds.size == 1) {
            output.append(" : ", KaTextAttribute.Punctuation)
            render(value.upperBounds.single(), KaPiece.Type)
        }
        return true
    }
}

private object WhereClauseRenderer : KaPieceRenderer<List<KaTypeParameterSymbol>>(KaPiece.WhereClause) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: List<KaTypeParameterSymbol>, next: () -> Unit): Boolean {
        // Only type parameters with more than one upper bound contribute to the `where` clause; single bounds are rendered inline.
        val constrainedTypeParameters = value.filter { it.upperBounds.size > 1 }
        if (constrainedTypeParameters.isEmpty()) return true

        output.space()
        keyword(KtTokens.WHERE_KEYWORD)
        var first = true
        for (typeParameter in constrainedTypeParameters) {
            for (bound in typeParameter.upperBounds) {
                if (!first) output.append(", ", KaTextAttribute.Punctuation)
                first = false
                render(typeParameter, KaPiece.SymbolName)
                output.append(" : ", KaTextAttribute.Punctuation)
                render(bound, KaPiece.Type)
            }
        }

        return true
    }
}

private object CallableWhereClauseRenderer : KaPieceRenderer<KaCallableSymbol>(KaPiece.CallableWhereClause) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaCallableSymbol, next: () -> Unit): Boolean {
        render(value.typeParameters, KaPiece.WhereClause)
        return true
    }
}

private object ClassifierWhereClauseRenderer : KaPieceRenderer<KaClassLikeSymbol>(KaPiece.ClassifierWhereClause) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaClassLikeSymbol, next: () -> Unit): Boolean {
        render(value.typeParameters, KaPiece.WhereClause)
        return true
    }
}

// -------------------------------------------------------------------------------------------------
// Types: top-level dispatcher.
// -------------------------------------------------------------------------------------------------

/**
 * Dispatches a type to the piece of its kind, after applying the options which affect every rendered type.
 *
 * The renderer prints nothing of its own: the annotations of a type are rendered by the piece its kind dispatches to, so that a piece which
 * renders another type in place of its own value does not print the annotations they share twice (see [renderTypeAnnotations]).
 */
private object TypeRenderer : KaPieceRenderer<KaType>(KaPiece.Type) {
    @OptIn(KaExperimentalApi::class)
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaType, next: () -> Unit): Boolean {
        val type = effectiveType(value)

        when (context.valueFor(KaRenderingOption.ClassTypeRenderingMode)) {
            KaClassTypeRenderingMode.ABBREVIATION -> {
                renderTypeAsIs(type.abbreviationOrSelf)
            }

            KaClassTypeRenderingMode.ABBREVIATION_WITH_EXPANSION_COMMENT -> {
                renderTypeAsIs(type.abbreviationOrSelf)
                expansionForComment(type)?.let { renderTypeComment("= ", it) }
            }

            KaClassTypeRenderingMode.EXPANSION -> {
                renderTypeAsIs(type.fullyExpandedType)
            }

            KaClassTypeRenderingMode.EXPANSION_WITH_ABBREVIATION_COMMENT -> {
                renderTypeAsIs(type.fullyExpandedType)
                abbreviationForComment(type)?.let { renderTypeComment("from: ", it) }
            }
        }
        return true
    }

    /** Renders [type] itself, without applying [KaRenderingOption.ClassTypeRenderingMode] to it again. */
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    private fun renderTypeAsIs(type: KaType) {
        when (type) {
            is KaFunctionType -> if (type.isRenderedAsFunctionType) {
                render(type, KaPiece.FunctionType)
            } else {
                // A reflection function type has no function type syntax, so it is rendered as the class type it is.
                render(type, KaPiece.ClassType)
            }

            is KaErrorType -> render(type, KaPiece.ErrorType)
            is KaClassType -> render(type, KaPiece.ClassType)
            is KaTypeParameterType -> render(type, KaPiece.TypeParameterType)
            is KaCapturedType -> render(type, KaPiece.CapturedType)
            is KaDefinitelyNotNullType -> render(type, KaPiece.DefinitelyNotNullType)
            is KaFlexibleType -> render(type, KaPiece.FlexibleType)
            is KaIntersectionType -> render(type, KaPiece.IntersectionType)
            is KaDynamicType -> render(type, KaPiece.DynamicType)
            else -> output.append("ERROR", KaTextAttribute.Identifier)
        }
    }

    /** The expansion to show in a comment next to [type], or `null` if [type] has no expansion of its own. */
    context(session: KaSession)
    private fun expansionForComment(type: KaType): KaType? = when {
        type.abbreviation != null -> type
        type.symbol is KaTypeAliasSymbol -> type.fullyExpandedType
        else -> null
    }

    /** The abbreviation to show in a comment next to the expansion of [type], or `null` if [type] has no abbreviation. */
    private fun abbreviationForComment(type: KaType): KaType? =
        type.abbreviation ?: type.takeIf { it.symbol is KaTypeAliasSymbol }

    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    private fun renderTypeComment(prefix: String, type: KaType) {
        output.append(" /* $prefix", KaTextAttribute.Comment)
        renderTypeAsIs(type)
        output.append(" */", KaTextAttribute.Comment)
    }

    /**
     * Applies [KaRenderingOption.TypeTransformation] and [KaRenderingOption.TypeApproximation] to [type].
     *
     * Every rendered type passes through [KaPiece.Type], so applying the options here covers all of them, including nested ones such as
     * type arguments. Approximation of an outer type already makes its arguments denotable, so the nested applications are no-ops.
     */
    @OptIn(KaExperimentalApi::class)
    context(session: KaSession, context: KaRenderingContext)
    private fun effectiveType(type: KaType): KaType {
        val transformed = context.valueFor(KaRenderingOption.TypeTransformation)(session, context, type)

        return when (context.valueFor(KaRenderingOption.TypeApproximation)) {
            KaTypeApproximation.NONE -> transformed
            KaTypeApproximation.TO_DENOTABLE_SUBTYPE -> transformed.approximateToDenotableSubtypeOrSelf()
            KaTypeApproximation.TO_DENOTABLE_SUPERTYPE -> transformed.approximateToDenotableSupertypeOrSelf(allowLocalDenotableTypes = true)
        }
    }
}

private object TypeAnnotationsRenderer : KaPieceRenderer<KaType>(KaPiece.TypeAnnotations) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaType, next: () -> Unit): Boolean {
        render(value, KaPiece.Annotations)
        return true
    }
}

private object TypeNullabilityRenderer : KaPieceRenderer<KaType>(KaPiece.TypeNullability) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaType, next: () -> Unit): Boolean {
        if (value.isMarkedNullable) output.append("?", KaTextAttribute.Punctuation)
        return true
    }
}

private object ClassTypeRenderer : KaPieceRenderer<KaClassType>(KaPiece.ClassType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaClassType, next: () -> Unit): Boolean {
        renderTypeAnnotations(value)
        render(value, KaPiece.ClassTypeName)
        render(value, KaPiece.TypeNullability)
        return true
    }
}

private object ClassTypeNameRenderer : KaPieceRenderer<KaClassType>(KaPiece.ClassTypeName) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaClassType, next: () -> Unit): Boolean {
        renderClassTypeQualifiedName(value)
        return true
    }
}

private object FunctionTypeRenderer : KaPieceRenderer<KaFunctionType>(KaPiece.FunctionType) {
    @OptIn(KaExperimentalApi::class)
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaFunctionType, next: () -> Unit): Boolean {
        renderTypeAnnotations(value)

        val isParenthesized = value.isMarkedNullable || hasRenderedAnnotations(value)
        if (isParenthesized) output.append("(", KaTextAttribute.GroupStart)

        if (value.isSuspend) keyword(KtTokens.SUSPEND_KEYWORD)
        renderFunctionTypeFamilyPrefix(value)

        val contextReceivers = value.contextReceivers
        if (contextReceivers.isNotEmpty()) {
            keyword(KtTokens.CONTEXT_KEYWORD, trailingSpace = false)
            output.append("(", KaTextAttribute.GroupStart)
            contextReceivers.forEachIndexed { index, contextReceiver ->
                if (index > 0) output.append(", ", KaTextAttribute.Punctuation)
                render(contextReceiver, KaPiece.FunctionTypeContextParameter)
            }
            output.append(")", KaTextAttribute.GroupEnd)
            output.space()
        }

        value.receiverType?.let { receiverType ->
            renderReceiverType(receiverType)
            output.append(".", KaTextAttribute.Punctuation)
        }

        output.append("(", KaTextAttribute.GroupStart)
        value.parameters.forEachIndexed { index, parameter ->
            if (index > 0) output.append(", ", KaTextAttribute.Punctuation)
            render(parameter, KaPiece.FunctionTypeParameter)
        }
        output.append(")", KaTextAttribute.GroupEnd)

        output.append(" -> ", KaTextAttribute.Punctuation)
        render(value.returnType, KaPiece.Type)

        if (isParenthesized) output.append(")", KaTextAttribute.GroupEnd)
        render(value, KaPiece.TypeNullability)
        return true
    }

    @OptIn(KaExperimentalApi::class)
    context(session: KaSession, context: KaRenderingContext)
    private fun hasRenderedAnnotations(type: KaFunctionType): Boolean =
        type.annotations.isNotEmpty() &&
                context.valueFor(KaRenderingOption.Annotations)(session, context, type).isNotEmpty()

    /**
     * Renders the [prefix][KaFunctionTypeFamily.typeRenderingPrefix] of the type's function type family, such as `@Composable` for the
     * family provided by the Compose compiler plugin.
     *
     * Nothing is rendered for a family without a prefix, for the `suspend` prefix, which is rendered as a keyword instead, and for a type
     * which carries the family's [marker annotation][KaFunctionTypeFamily.markerAnnotationClassId], as that annotation is already rendered
     * as a type annotation.
     */
    @OptIn(KaExperimentalApi::class)
    context(session: KaSession, output: KaRenderingOutput)
    private fun renderFunctionTypeFamilyPrefix(type: KaFunctionType) {
        val family = type.functionTypeFamily ?: return
        val prefix = family.typeRenderingPrefix ?: return
        if (prefix == KtTokens.SUSPEND_KEYWORD.value) return

        val markerAnnotationClassId = family.markerAnnotationClassId
        if (markerAnnotationClassId != null && type.annotations.any { it.classId == markerAnnotationClassId }) return

        output.append(prefix, KaTextAttribute.Identifier)
        output.space()
    }
}

private object FunctionTypeParameterRenderer : KaPieceRenderer<KaFunctionValueParameter>(KaPiece.FunctionTypeParameter) {
    @OptIn(KaExperimentalApi::class)
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaFunctionValueParameter, next: () -> Unit): Boolean {
        // A function type parameter is named by a `@ParameterName` annotation on its type, so most parameters have no name at all.
        value.name?.let { name ->
            output.append(name.render(), KaTextAttribute.Identifier)
            output.append(": ", KaTextAttribute.Punctuation)
        }

        render(value.type, KaPiece.Type)
        return true
    }
}

private object FunctionTypeContextParameterRenderer :
    KaPieceRenderer<KaContextReceiver>(KaPiece.FunctionTypeContextParameter) {
    @OptIn(KaExperimentalApi::class)
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaContextReceiver, next: () -> Unit): Boolean {
        render(value.type, KaPiece.Type)
        return true
    }
}

private object TypeParameterTypeRenderer : KaPieceRenderer<KaTypeParameterType>(KaPiece.TypeParameterType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaTypeParameterType, next: () -> Unit): Boolean {
        renderTypeAnnotations(value)
        identifier(value.name, value.symbol)
        render(value, KaPiece.TypeNullability)
        return true
    }
}

private object CapturedTypeRenderer : KaPieceRenderer<KaCapturedType>(KaPiece.CapturedType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaCapturedType, next: () -> Unit): Boolean {
        renderTypeAnnotations(value)
        output.append("Captured", KaTextAttribute.Identifier)
        output.append("(", KaTextAttribute.GroupStart)
        render(value.projection, KaPiece.TypeProjection)
        output.append(")", KaTextAttribute.GroupEnd)
        return true
    }
}

private object DefinitelyNotNullTypeRenderer : KaPieceRenderer<KaDefinitelyNotNullType>(KaPiece.DefinitelyNotNullType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaDefinitelyNotNullType, next: () -> Unit): Boolean {
        // The original type carries the same annotations, so it renders them.
        render(value.original, KaPiece.Type)
        output.append(" & ", KaTextAttribute.Punctuation)
        output.append("Any", KaTextAttribute.Identifier)
        return true
    }
}

private object FlexibleTypeRenderer : KaPieceRenderer<KaFlexibleType>(KaPiece.FlexibleType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaFlexibleType, next: () -> Unit): Boolean {
        val lower = value.lowerBound
        val upper = value.upperBound

        if (context.valueFor(KaRenderingOption.FlexibleTypeShrinking)) {
            // A flexible type whose bounds differ only in nullability (e.g. `String..String?`) is rendered compactly as `String!`.
            // The lower bound carries the same annotations as the flexible type, so it renders them.
            if (isNullabilityFlexibleType(lower, upper)) {
                render(lower, KaPiece.Type)
                output.append("!", KaTextAttribute.Punctuation)
                return true
            }

            // A flexible type between a mutable collection and its read-only counterpart (e.g. `MutableList<String!>..List<String!>?`)
            // is rendered compactly as `(Mutable)List<String!>!`. Unlike the nullability case, the lower bound cannot be rendered as a
            // whole type, as its classifier name carries the marker, so only its type arguments are rendered. A difference between the
            // bounds' type arguments, should one occur, is therefore not visible in the output.
            if (lower is KaClassType && upper is KaClassType && isMutabilityFlexibleType(lower, upper)) {
                // No bound is rendered as a type here, so this is the one form which renders the flexible type's own annotations.
                renderTypeAnnotations(value)
                renderClassTypeQualifiedName(lower, mutabilityFlexible = true)
                if (value.hasFlexibleNullability) {
                    output.append("!", KaTextAttribute.Punctuation)
                } else {
                    // Both bounds agree on nullability, so it is rendered as it is, e.g. `(Mutable)List<String!>?`.
                    render(lower, KaPiece.TypeNullability)
                }
                return true
            }
        }

        // Each bound renders its own annotations, which for the lower bound are those of the flexible type.
        output.append("(", KaTextAttribute.GroupStart)
        render(lower, KaPiece.Type)
        output.append("..", KaTextAttribute.Punctuation)
        render(upper, KaPiece.Type)
        output.append(")", KaTextAttribute.GroupEnd)
        return true
    }

    context(session: KaSession)
    private fun isNullabilityFlexibleType(lower: KaType, upper: KaType): Boolean {
        val isSameType = lower is KaClassType && upper is KaClassType && lower.classId == upper.classId ||
                lower is KaTypeParameterType && upper is KaTypeParameterType && lower.symbol == upper.symbol
        if (!isSameType || lower.isMarkedNullable || !upper.isMarkedNullable) return false

        // For class types, the bounds must additionally share the same type arguments.
        if (lower is KaClassType && upper is KaClassType) {
            val lowerArguments = lower.typeArguments
            val upperArguments = upper.typeArguments
            return lowerArguments.size == upperArguments.size &&
                    lowerArguments.indices.all { upperArguments[it].type == lowerArguments[it].type }
        }

        return true
    }

    /** Whether the bounds are a mutable collection and its read-only counterpart, such as `MutableList<String>` and `List<String>`. */
    private fun isMutabilityFlexibleType(lower: KaClassType, upper: KaClassType): Boolean =
        StandardClassIds.Collections.mutableCollectionToBaseCollection[lower.classId] == upper.classId
}

private object IntersectionTypeRenderer : KaPieceRenderer<KaIntersectionType>(KaPiece.IntersectionType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaIntersectionType, next: () -> Unit): Boolean {
        // An annotation shared by all conjuncts is an annotation of the intersection type as well, so the conjuncts render them.
        value.conjuncts.forEachIndexed { index, conjunct ->
            if (index > 0) output.append(" & ", KaTextAttribute.Punctuation)
            render(conjunct, KaPiece.Type)
        }
        return true
    }
}

private object DynamicTypeRenderer : KaPieceRenderer<KaDynamicType>(KaPiece.DynamicType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaDynamicType, next: () -> Unit): Boolean {
        renderTypeAnnotations(value)
        keyword(KtTokens.DYNAMIC_KEYWORD, trailingSpace = false)
        return true
    }
}

private object ErrorTypeRenderer : KaPieceRenderer<KaErrorType>(KaPiece.ErrorType) {
    @OptIn(KaNonPublicApi::class)
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaErrorType, next: () -> Unit): Boolean {
        renderTypeAnnotations(value)

        // For unresolved class types, render the written form (with type arguments), e.g. `C<String>`, instead of just an error marker.
        val qualifiers = (value as? KaClassErrorType)?.qualifiers
        if (!qualifiers.isNullOrEmpty()) {
            qualifiers.forEachIndexed { index, qualifier ->
                if (index > 0) output.append(".", KaTextAttribute.Punctuation)
                val symbol = (qualifier as? KaResolvedClassTypeQualifier)?.symbol
                if (symbol != null) {
                    identifier(qualifier.name, symbol)
                } else {
                    output.append(qualifier.name.render(), KaTextAttribute.Identifier)
                }
                if (qualifier.typeArguments.isNotEmpty()) {
                    render(qualifier.typeArguments, KaPiece.TypeArgumentList)
                }
            }
            return true
        }

        output.append(value.presentableText ?: "ERROR", KaTextAttribute.Identifier)
        return true
    }
}

// -------------------------------------------------------------------------------------------------
// Type projections.
// -------------------------------------------------------------------------------------------------

private object TypeArgumentListRenderer : KaPieceRenderer<List<KaTypeProjection>>(KaPiece.TypeArgumentList) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: List<KaTypeProjection>, next: () -> Unit): Boolean {
        output.append("<", KaTextAttribute.GroupStart)
        value.forEachIndexed { index, projection ->
            if (index > 0) output.append(", ", KaTextAttribute.Punctuation)
            render(projection, KaPiece.TypeProjection)
        }
        output.append(">", KaTextAttribute.GroupEnd)
        return true
    }
}

private object TypeProjectionRenderer : KaPieceRenderer<KaTypeProjection>(KaPiece.TypeProjection) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaTypeProjection, next: () -> Unit): Boolean {
        when (value) {
            is KaStarTypeProjection -> output.append("*", KaTextAttribute.Punctuation)
            is KaTypeArgumentWithVariance -> {
                varianceKeyword(value.variance)
                render(value.type, KaPiece.Type)
            }
        }
        return true
    }
}
