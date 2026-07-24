/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.rendering

import org.jetbrains.kotlin.analysis.api.KaExperimentalApi
import org.jetbrains.kotlin.analysis.api.KaNonPublicApi
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotated
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotation
import org.jetbrains.kotlin.analysis.api.annotations.KaAnnotationValue
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.rendering.KaClassTypeQualification
import org.jetbrains.kotlin.analysis.api.rendering.KaPiece
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderer
import org.jetbrains.kotlin.analysis.api.rendering.KaPieceRenderer
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOption
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOutput
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingContext
import org.jetbrains.kotlin.analysis.api.rendering.KaTextAttribute
import org.jetbrains.kotlin.analysis.api.rendering.KaTypeApproximation
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
import org.jetbrains.kotlin.analysis.api.types.KaIntersectionType
import org.jetbrains.kotlin.analysis.api.types.KaResolvedClassTypeQualifier
import org.jetbrains.kotlin.analysis.api.types.KaStarTypeProjection
import org.jetbrains.kotlin.analysis.api.types.KaType
import org.jetbrains.kotlin.analysis.api.types.KaTypeArgumentWithVariance
import org.jetbrains.kotlin.analysis.api.types.KaTypeParameterType
import org.jetbrains.kotlin.analysis.api.types.KaTypeProjection
import org.jetbrains.kotlin.analysis.api.types.approximateToDenotableSubtypeOrSelf
import org.jetbrains.kotlin.analysis.api.types.approximateToDenotableSupertypeOrSelf
import org.jetbrains.kotlin.analysis.api.types.classId
import org.jetbrains.kotlin.analysis.api.types.expandedSymbol
import org.jetbrains.kotlin.analysis.api.types.isMarkedNullable
import org.jetbrains.kotlin.analysis.api.types.isNullable
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.name.render
import org.jetbrains.kotlin.types.Variance

internal val DEFAULT_RENDERER: KaRenderer = buildRenderer(null) {
    // Top-level dispatchers.
    push(SymbolRenderer)
    push(TypeRenderer)

    // Shared building blocks.
    push(SymbolNameRenderer)
    push(SymbolModifiersRenderer)
    push(VisibilityRenderer)
    push(ModalityRenderer)
    push(AnnotationsRenderer)
    push(AnnotationRenderer)
    push(AnnotationValuesRenderer)
    push(AnnotationValueRenderer)
    push(ConstantValueRenderer)
    push(TypeAnnotationsRenderer)
    push(TypeNullabilityRenderer)

    // Types.
    push(ClassTypeRenderer)
    push(FunctionTypeRenderer)
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

    // Functions.
    push(FunctionRenderer)
    push(FunctionModifiersRenderer)
    push(FunctionAnnotationsRenderer)
    push(FunctionReceiverRenderer)
    push(FunctionValueParameterListRenderer)
    push(FunctionReturnTypeRenderer)
    push(NamedFunctionReturnTypeRenderer)
    push(NamedFunctionRenderer)
    push(ConstructorRenderer)
    push(PropertyAccessorRenderer)

    // Properties and variables.
    push(PropertyRenderer)
    push(PropertyModifiersRenderer)
    push(PropertyAnnotationsRenderer)
    push(PropertyReturnTypeRenderer)
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
    push(ClassModifiersRenderer)
    push(ClassAnnotationsRenderer)
    push(SupertypeListRenderer)
    push(SupertypeRenderer)
    push(ClassBodyRenderer)

    // Packages.
    push(PackageRenderer)
}

// -------------------------------------------------------------------------------------------------
// Shared helpers.
// -------------------------------------------------------------------------------------------------

context(output: KaRenderingOutput)
private fun keyword(text: String) {
    output.append(text, KaTextAttribute.Keyword).space()
}

/**
 * The visibility of [symbol] as it is written in source code. A primary constructor of an enum class or an object is implicitly private,
 * so its visibility is reported as public and is not rendered.
 */
context(session: KaSession)
private fun effectiveVisibility(symbol: KaDeclarationSymbol): KaSymbolVisibility {
    if (symbol is KaConstructorSymbol && symbol.isPrimary) {
        val containingClass = symbol.containingDeclaration as? KaClassSymbol
        if (containingClass != null && (containingClass.classKind == KaClassKind.ENUM_CLASS || containingClass.classKind.isObject)) {
            return KaSymbolVisibility.PUBLIC
        }
    }
    return symbol.visibility
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
 * Renders a package fully-qualified name segment by segment (e.g. `org.example`), linking each segment to the [KaPackageSymbol] of the
 * package it forms, when that package can be resolved.
 */
context(session: KaSession, output: KaRenderingOutput)
private fun renderPackageQualifier(packageFqName: FqName) {
    var current = FqName.ROOT
    packageFqName.pathSegments().forEachIndexed { index, segment ->
        if (index > 0) output.append(".", KaTextAttribute.Punctuation)
        current = current.child(segment)
        val packageSymbol = findPackage(current)
        if (packageSymbol != null) {
            identifier(segment, packageSymbol)
        } else {
            output.append(segment.render(), KaTextAttribute.Identifier)
        }
    }
}

/**
 * Renders a receiver type of extension callable or a function type, wrapping it in parentheses when required by the Kotlin grammar
 * (function types, definitely non-nullable types, and nullable types).
 */
context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
private fun renderReceiverType(type: KaType) {
    val parenthesized = type is KaFunctionType || type is KaDefinitelyNotNullType || type.isMarkedNullable
    if (parenthesized) output.append("(", KaTextAttribute.GroupStart)
    render(type, KaPiece.Type)
    if (parenthesized) output.append(")", KaTextAttribute.GroupEnd)
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
            is KaPackageSymbol -> identifier(value.fqName.render(), value)
            is KaNamedSymbol -> identifier(value.name, value)
            else -> output.append("<symbol>", KaTextAttribute.Identifier)
        }
        return true
    }
}

private object SymbolModifiersRenderer : KaPieceRenderer<KaDeclarationSymbol>(KaPiece.SymbolModifiers) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaDeclarationSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.Visibility)
        if (value.isExpect) keyword("expect")
        if (value.isActual) keyword("actual")
        if (value.isExternal) keyword("external")
        return true
    }
}

private object VisibilityRenderer : KaPieceRenderer<KaDeclarationSymbol>(KaPiece.Visibility) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaDeclarationSymbol, next: () -> Unit): Boolean {
        val text = when (effectiveVisibility(value)) {
            KaSymbolVisibility.PRIVATE -> "private"
            KaSymbolVisibility.PROTECTED -> "protected"
            KaSymbolVisibility.INTERNAL -> "internal"
            else -> return true
        }
        keyword(text)
        return true
    }
}

private object ModalityRenderer : KaPieceRenderer<KaDeclarationSymbol>(KaPiece.Modality) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaDeclarationSymbol, next: () -> Unit): Boolean {
        if (!shouldRenderModality(value)) return true

        val text = when (value.modality) {
            KaSymbolModality.ABSTRACT -> "abstract"
            KaSymbolModality.OPEN -> "open"
            KaSymbolModality.SEALED -> "sealed"
            KaSymbolModality.FINAL -> return true
        }
        keyword(text)
        return true
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

private object AnnotationsRenderer : KaPieceRenderer<KaAnnotated>(KaPiece.Annotations) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaAnnotated, next: () -> Unit): Boolean {
        val annotations = value.annotations
        if (annotations.isEmpty()) return true

        val onNewLine = context.valueFor(KaRenderingOption.AnnotationsOnNewLine)(session, context, value)
        for (annotation in annotations) {
            render(annotation, KaPiece.Annotation)
            if (onNewLine) output.newLine() else output.space()
        }
        return true
    }
}

private object AnnotationRenderer : KaPieceRenderer<KaAnnotation>(KaPiece.Annotation) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaAnnotation, next: () -> Unit): Boolean {
        output.append("@", KaTextAttribute.Punctuation)
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
                renderPackageQualifier(packageFqName)
                output.append(".", KaTextAttribute.Punctuation)
            }
        }

        // `SIMPLE` keeps only the innermost segment; the other modes render all outer classifiers.
        val segments = when (qualification) {
            KaClassTypeQualification.SIMPLE -> listOf(classId.shortClassName)
            else -> classId.relativeClassName.pathSegments()
        }

        segments.forEachIndexed { index, segment ->
            if (index > 0) output.append(".", KaTextAttribute.Punctuation)
            output.append(segment.render(), KaTextAttribute.Identifier)
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
        render(value, KaPiece.FunctionModifiers)
        keyword("fun")

        if (value.typeParameters.isNotEmpty()) {
            render(value, KaPiece.CallableTypeParameterList)
            output.space()
        }

        value.receiverParameter?.let { render(it, KaPiece.FunctionReceiver) }

        render(value, KaPiece.SymbolName)

        render(value, KaPiece.FunctionValueParameterList)

        render(value, KaPiece.NamedFunctionReturnType)

        render(value, KaPiece.CallableWhereClause)

        return true
    }
}

/** Generic function renderer used for anonymous functions and SAM constructors. */
private object FunctionRenderer : KaPieceRenderer<KaFunctionSymbol>(KaPiece.Function) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaFunctionSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.FunctionModifiers)
        keyword("fun")

        value.receiverParameter?.let { render(it, KaPiece.FunctionReceiver) }

        if (value is KaNamedSymbol) {
            render(value, KaPiece.SymbolName)
        }

        render(value, KaPiece.FunctionValueParameterList)

        render(value, KaPiece.FunctionReturnType)
        return true
    }
}

private object ConstructorRenderer : KaPieceRenderer<KaConstructorSymbol>(KaPiece.Constructor) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaConstructorSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.Annotations)
        render(value, KaPiece.Visibility)
        output.append("constructor", KaTextAttribute.Keyword)
        render(value, KaPiece.FunctionValueParameterList)
        return true
    }
}

private object PrimaryConstructorRenderer : KaPieceRenderer<KaConstructorSymbol>(KaPiece.PrimaryConstructor) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaConstructorSymbol, next: () -> Unit): Boolean {
        // The `constructor` keyword is rendered when the primary constructor carries its own annotations or an explicit,
        // non-default (non-public) visibility.
        if (value.annotations.isNotEmpty() || effectiveVisibility(value) != KaSymbolVisibility.PUBLIC) {
            output.space()
            render(value, KaPiece.Annotations)
            render(value, KaPiece.Visibility)
            output.append("constructor", KaTextAttribute.Keyword)
        }

        val parameters = value.valueParameters
        if (parameters.isNotEmpty()) {
            output.append("(", KaTextAttribute.GroupStart)
            parameters.forEachIndexed { index, parameter ->
                if (index > 0) output.append(", ", KaTextAttribute.Punctuation)
                renderSignatureParameter(parameter)
            }
            output.append(")", KaTextAttribute.GroupEnd)
        }
        return true
    }

    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    private fun renderSignatureParameter(parameter: KaValueParameterSymbol) {
        if (parameter.isVararg) keyword("vararg")
        render(parameter, KaPiece.SymbolName)
        output.append(": ", KaTextAttribute.Punctuation)
        render(parameter, KaPiece.ValueParameterType)
    }
}

private object PropertyAccessorRenderer : KaPieceRenderer<KaPropertyAccessorSymbol>(KaPiece.PropertyAccessor) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaPropertyAccessorSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.Visibility)
        val isGetter = value is KaPropertyGetterSymbol
        output.append(if (isGetter) "get" else "set", KaTextAttribute.Keyword)
        output.append("(", KaTextAttribute.GroupStart)
        if (value is KaPropertySetterSymbol) {
            render(value.parameter, KaPiece.ValueParameter)
        }
        output.append(")", KaTextAttribute.GroupEnd)
        return true
    }
}

private object FunctionModifiersRenderer : KaPieceRenderer<KaFunctionSymbol>(KaPiece.FunctionModifiers) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaFunctionSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.SymbolModifiers)
        render(value, KaPiece.Modality)
        if (value is KaNamedFunctionSymbol) {
            if (value.isOverride) keyword("override")
            if (value.isTailRec) keyword("tailrec")
            if (value.isSuspend) keyword("suspend")
            if (value.isInline) keyword("inline")
            if (value.isInfix) keyword("infix")
            if (value.isOperator) keyword("operator")
        }
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

        if (value.isVararg) keyword("vararg")
        if (value.isCrossinline) keyword("crossinline")
        if (value.isNoinline) keyword("noinline")

        value.primaryConstructorProperty?.let { property ->
            keyword(if (property.isVal) "val" else "var")
        }

        render(value, KaPiece.SymbolName)
        output.append(": ", KaTextAttribute.Punctuation)
        render(value, KaPiece.ValueParameterType)
        render(value, KaPiece.ValueParameterDefaultValue)
        return true
    }
}

private object ValueParameterAnnotationsRenderer : KaPieceRenderer<KaValueParameterSymbol>(KaPiece.ValueParameterAnnotations) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaValueParameterSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.Annotations)
        return true
    }
}

private object ValueParameterTypeRenderer : KaPieceRenderer<KaValueParameterSymbol>(KaPiece.ValueParameterType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaValueParameterSymbol, next: () -> Unit): Boolean {
        render(value.returnType, KaPiece.Type)
        return true
    }
}

private object ValueParameterDefaultValueRenderer : KaPieceRenderer<KaValueParameterSymbol>(KaPiece.ValueParameterDefaultValue) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaValueParameterSymbol, next: () -> Unit): Boolean {
        if (!value.hasDefaultValue) return true

        // The default value expression is not part of the symbol, so it is rendered as a placeholder.
        output.append(" = ", KaTextAttribute.Punctuation)
        output.append("...", KaTextAttribute.Punctuation)
        return true
    }
}

// -------------------------------------------------------------------------------------------------
// Context parameters.
// -------------------------------------------------------------------------------------------------

private object ContextParameterListRenderer : KaPieceRenderer<List<KaContextParameterSymbol>>(KaPiece.ContextParameterList) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: List<KaContextParameterSymbol>, next: () -> Unit): Boolean {
        output.append("context", KaTextAttribute.Keyword)
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
        render(value, KaPiece.SymbolName)
        output.append(": ", KaTextAttribute.Punctuation)
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
        render(value, KaPiece.PropertyModifiers)
        keyword(if (value.isVal) "val" else "var")

        if (value.typeParameters.isNotEmpty()) {
            render(value, KaPiece.CallableTypeParameterList)
            output.space()
        }

        value.receiverParameter?.let { render(it, KaPiece.FunctionReceiver) }

        render(value, KaPiece.SymbolName)
        output.append(": ", KaTextAttribute.Punctuation)
        render(value, KaPiece.PropertyReturnType)

        render(value, KaPiece.CallableWhereClause)

        render(value, KaPiece.PropertyAccessors)
        return true
    }
}

private object PropertyAccessorsRenderer : KaPieceRenderer<KaPropertySymbol>(KaPiece.PropertyAccessors) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaPropertySymbol, next: () -> Unit): Boolean {
        val getter = value.getter?.takeIf { it.isNotDefault }
        val setter = value.setter?.takeIf { it.isNotDefault }
        if (getter == null && setter == null) return true

        output.indent()
        getter?.let { output.newLine(); render(it, KaPiece.PropertyAccessor) }
        setter?.let { output.newLine(); render(it, KaPiece.PropertyAccessor) }
        output.unindent()
        return true
    }
}

private object PropertyModifiersRenderer : KaPieceRenderer<KaPropertySymbol>(KaPiece.PropertyModifiers) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaPropertySymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.SymbolModifiers)
        render(value, KaPiece.Modality)
        if (value.isOverride) keyword("override")
        if (value is KaKotlinPropertySymbol) {
            if (value.isConst) keyword("const")
            if (value.isLateInit) keyword("lateinit")
        }
        return true
    }
}

private object PropertyAnnotationsRenderer : KaPieceRenderer<KaPropertySymbol>(KaPiece.PropertyAnnotations) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaPropertySymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.Annotations)
        return true
    }
}

private object PropertyReturnTypeRenderer : KaPieceRenderer<KaPropertySymbol>(KaPiece.PropertyReturnType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaPropertySymbol, next: () -> Unit): Boolean {
        render(value.returnType, KaPiece.Type)
        return true
    }
}

private object LocalVariableRenderer : KaPieceRenderer<KaLocalVariableSymbol>(KaPiece.LocalVariable) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaLocalVariableSymbol, next: () -> Unit): Boolean {
        if (value.isLateInit) keyword("lateinit")
        keyword(if (value.isVal) "val" else "var")
        render(value, KaPiece.SymbolName)
        output.append(": ", KaTextAttribute.Punctuation)
        render(value.returnType, KaPiece.Type)
        return true
    }
}

private object JavaFieldRenderer : KaPieceRenderer<KaJavaFieldSymbol>(KaPiece.JavaField) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaJavaFieldSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.Visibility)
        if (value.isStatic) keyword("static")
        keyword(if (value.isVal) "val" else "var")
        render(value, KaPiece.SymbolName)
        output.append(": ", KaTextAttribute.Punctuation)
        render(value.returnType, KaPiece.Type)
        return true
    }
}

private object BackingFieldRenderer : KaPieceRenderer<KaBackingFieldSymbol>(KaPiece.BackingField) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaBackingFieldSymbol, next: () -> Unit): Boolean {
        output.append("field", KaTextAttribute.Keyword)
        output.append(": ", KaTextAttribute.Punctuation)
        render(value.returnType, KaPiece.Type)
        return true
    }
}

private object EnumEntryRenderer : KaPieceRenderer<KaEnumEntrySymbol>(KaPiece.EnumEntry) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaEnumEntrySymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.Annotations)
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
        render(value, KaPiece.ClassModifiers)

        val classKeyword = when (value.classKind) {
            KaClassKind.INTERFACE -> "interface"
            KaClassKind.OBJECT, KaClassKind.COMPANION_OBJECT -> "object"
            else -> "class"
        }
        output.append(classKeyword, KaTextAttribute.Keyword)

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
        output.append("object", KaTextAttribute.Keyword)
        render(value, KaPiece.SupertypeList)
        render(value, KaPiece.ClassBody)
        return true
    }
}

private object TypeAliasRenderer : KaPieceRenderer<KaTypeAliasSymbol>(KaPiece.TypeAlias) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaTypeAliasSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.SymbolModifiers)
        keyword("typealias")
        render(value, KaPiece.SymbolName)
        if (value.typeParameters.isNotEmpty()) {
            render(value, KaPiece.ClassifierTypeParameterList)
        }
        output.append(" = ", KaTextAttribute.Punctuation)
        render(value.expandedType, KaPiece.Type)
        return true
    }
}

private object ClassModifiersRenderer : KaPieceRenderer<KaNamedClassSymbol>(KaPiece.ClassModifiers) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaNamedClassSymbol, next: () -> Unit): Boolean {
        render(value, KaPiece.SymbolModifiers)
        render(value, KaPiece.Modality)

        when (value.classKind) {
            KaClassKind.COMPANION_OBJECT -> keyword("companion")
            KaClassKind.ENUM_CLASS -> keyword("enum")
            KaClassKind.ANNOTATION_CLASS -> keyword("annotation")
            else -> {}
        }

        if (value.isInner) keyword("inner")
        if (value.isData) keyword("data")
        if (value.isInline) keyword("value")
        if (value.isFun) keyword("fun")
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
        keyword("package")
        render(value, KaPiece.SymbolName)
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
        if (value.isReified) keyword("reified")
        if (value.variance != Variance.INVARIANT) keyword(value.variance.label)

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
        keyword("where")
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

private object TypeRenderer : KaPieceRenderer<KaType>(KaPiece.Type) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaType, next: () -> Unit): Boolean {
        val type = effectiveType(value)

        if (type.annotations.isNotEmpty()) {
            render(type, KaPiece.TypeAnnotations)
        }

        // If the type is a type-alias application, render the alias (e.g. `WithGeneric<Double>`) rather than its expansion.
        val abbreviation = type.abbreviation
        if (abbreviation != null) {
            render(abbreviation, KaPiece.ClassType)
            return true
        }

        when (type) {
            is KaFunctionType -> render(type, KaPiece.FunctionType)
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
        return true
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
        val qualification = context.valueFor(KaRenderingOption.ClassTypeQualification)
        val qualifiers = value.qualifiers

        if (qualifiers.isEmpty()) {
            // Fallback for class types that expose no explicit qualifier segments.
            identifier(value.classId.shortClassName, value.symbol)
            if (value.typeArguments.isNotEmpty()) {
                render(value.typeArguments, KaPiece.TypeArgumentList)
            }
        } else {
            if (qualification == KaClassTypeQualification.FULLY_QUALIFIED) {
                val packageFqName = value.classId.packageFqName
                if (!packageFqName.isRoot && packageFqName != CallableId.PACKAGE_FQ_NAME_FOR_LOCAL) {
                    renderPackageQualifier(packageFqName)
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
                identifier(qualifier.name, qualifier.symbol)
                if (qualifier.typeArguments.isNotEmpty()) {
                    render(qualifier.typeArguments, KaPiece.TypeArgumentList)
                }
            }
        }

        render(value, KaPiece.TypeNullability)
        return true
    }
}

private object FunctionTypeRenderer : KaPieceRenderer<KaFunctionType>(KaPiece.FunctionType) {
    @OptIn(KaExperimentalApi::class)
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaFunctionType, next: () -> Unit): Boolean {
        val nullable = value.isMarkedNullable
        if (nullable) output.append("(", KaTextAttribute.GroupStart)

        if (value.isSuspend) keyword("suspend")

        val contextReceivers = value.contextReceivers
        if (contextReceivers.isNotEmpty()) {
            output.append("context", KaTextAttribute.Keyword)
            output.append("(", KaTextAttribute.GroupStart)
            contextReceivers.forEachIndexed { index, contextReceiver ->
                if (index > 0) output.append(", ", KaTextAttribute.Punctuation)
                render(contextReceiver.type, KaPiece.Type)
            }
            output.append(")", KaTextAttribute.GroupEnd)
            output.space()
        }

        value.receiverType?.let { receiverType ->
            renderReceiverType(receiverType)
            output.append(".", KaTextAttribute.Punctuation)
        }

        output.append("(", KaTextAttribute.GroupStart)
        value.parameterTypes.forEachIndexed { index, parameterType ->
            if (index > 0) output.append(", ", KaTextAttribute.Punctuation)
            render(parameterType, KaPiece.Type)
        }
        output.append(")", KaTextAttribute.GroupEnd)

        output.append(" -> ", KaTextAttribute.Punctuation)
        render(value.returnType, KaPiece.Type)

        if (nullable) output.append(")", KaTextAttribute.GroupEnd)
        render(value, KaPiece.TypeNullability)
        return true
    }
}

private object TypeParameterTypeRenderer : KaPieceRenderer<KaTypeParameterType>(KaPiece.TypeParameterType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaTypeParameterType, next: () -> Unit): Boolean {
        identifier(value.name, value.symbol)
        render(value, KaPiece.TypeNullability)
        return true
    }
}

private object CapturedTypeRenderer : KaPieceRenderer<KaCapturedType>(KaPiece.CapturedType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaCapturedType, next: () -> Unit): Boolean {
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

        // A flexible type whose bounds differ only in nullability (e.g. `String..String?`) is rendered compactly as `String!`.
        if (isNullabilityFlexibleType(lower, upper)) {
            render(lower, KaPiece.Type)
            output.append("!", KaTextAttribute.Punctuation)
            return true
        }

        render(lower, KaPiece.Type)
        output.append("..", KaTextAttribute.Punctuation)
        render(upper, KaPiece.Type)
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
}

private object IntersectionTypeRenderer : KaPieceRenderer<KaIntersectionType>(KaPiece.IntersectionType) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaIntersectionType, next: () -> Unit): Boolean {
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
        output.append("dynamic", KaTextAttribute.Keyword)
        return true
    }
}

private object ErrorTypeRenderer : KaPieceRenderer<KaErrorType>(KaPiece.ErrorType) {
    @OptIn(KaNonPublicApi::class)
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaErrorType, next: () -> Unit): Boolean {
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
                if (value.variance != Variance.INVARIANT) keyword(value.variance.label)
                render(value.type, KaPiece.Type)
            }
        }
        return true
    }
}
