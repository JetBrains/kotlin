/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.fir.rendering

import org.jetbrains.kotlin.analysis.api.KaConstantInitializerValue
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.rendering.KaPiece
import org.jetbrains.kotlin.analysis.api.rendering.KaPieceRenderer
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingOutput
import org.jetbrains.kotlin.analysis.api.rendering.KaRenderingContext
import org.jetbrains.kotlin.analysis.api.rendering.KaTextAttribute
import org.jetbrains.kotlin.analysis.api.rendering.append
import org.jetbrains.kotlin.analysis.api.rendering.pushEmpty
import org.jetbrains.kotlin.analysis.api.rendering.render
import org.jetbrains.kotlin.analysis.api.symbols.KaBackingFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaConstructorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaJavaFieldSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaKotlinPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaLocalVariableSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaNamedFunctionSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaParameterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyAccessorSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertyGetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySetterSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.markers.KaNamedSymbol
import org.jetbrains.kotlin.analysis.api.types.classId
import org.jetbrains.kotlin.analysis.api.types.isNullable
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.analysis.api.rendering.KaRendererBuilder

internal fun KaRendererBuilder.pushCallableRenderers() {
    push(FunctionRenderer)
    push(NamedFunctionRenderer)
    push(ConstructorRenderer)
    push(PropertyAccessorRenderer)
    push(FunctionAnnotationsRenderer)
    push(FunctionReceiverRenderer)
    push(FunctionValueParameterListRenderer)
    push(FunctionReturnTypeRenderer)
    push(NamedFunctionReturnTypeRenderer)
    pushEmpty(KaPiece.FunctionBody)
    pushEmpty(KaPiece.ConstructorBody)

    push(PropertyRenderer)
    push(PropertyAnnotationsRenderer)
    push(PropertyReturnTypeRenderer)
    push(PropertyInitializerRenderer)
    push(PropertyAccessorsRenderer)
    push(LocalVariableRenderer)
    push(JavaFieldRenderer)
    push(BackingFieldRenderer)
}

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
        render(value to null, KaPiece.Annotations)
        render(value, KaPiece.SymbolModifiers)
        keyword(KtTokens.CONSTRUCTOR_KEYWORD, trailingSpace = false)
        render(value, KaPiece.FunctionValueParameterList)
        render(value, KaPiece.ConstructorBody)

        return true
    }
}

private object PropertyAccessorRenderer : KaPieceRenderer<KaPropertyAccessorSymbol>(KaPiece.PropertyAccessor) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaPropertyAccessorSymbol, next: () -> Unit): Boolean {
        render(value to null, KaPiece.Annotations)
        render(value, KaPiece.SymbolModifiers)
        val isGetter = value is KaPropertyGetterSymbol
        keyword(if (isGetter) KtTokens.GET_KEYWORD else KtTokens.SET_KEYWORD, trailingSpace = false)

        // A default accessor has no parameter list in source code, as in `@Anno get` or `private set`.
        if (value.isNotDefault) {
            output.group(KaPiece.ValueParameter) {
                output.append("(", KaTextAttribute.Punctuation)
                if (value is KaPropertySetterSymbol) {
                    render(value.parameter, KaPiece.ValueParameter)
                }
                output.append(")", KaTextAttribute.Punctuation)
            }
        }

        render(value, KaPiece.FunctionBody)
        return true
    }
}

private object FunctionAnnotationsRenderer : KaPieceRenderer<KaFunctionSymbol>(KaPiece.FunctionAnnotations) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaFunctionSymbol, next: () -> Unit): Boolean {
        render(value to null, KaPiece.Annotations)
        return true
    }
}

private object FunctionReceiverRenderer : KaPieceRenderer<KaParameterSymbol>(KaPiece.FunctionReceiver) {
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaParameterSymbol, next: () -> Unit): Boolean {
        // The annotations of the receiver parameter itself can only be written with a use-site target.
        // An annotation written on the receiver type, as in `fun (@Anno String).foo()`, belongs  to the type instead.
        render(value to AnnotationUseSiteTarget.RECEIVER, KaPiece.Annotations)
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

        // An explicit backing field is rendered as a declaration of its own, on an indented line below the property.
        value.backingFieldSymbol?.takeIf { it.isNotDefault }?.let { backingField ->
            output.indent()
            output.newLine()
            render(backingField, KaPiece.BackingField)
            output.unindent()
        }

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
        render(value to null, KaPiece.Annotations)

        // The annotations of the accessors are rendered on the accessors themselves. A default backing field has no rendered
        // declaration, so its annotations are rendered on the property with a use-site target. An explicit backing field is rendered
        // as its own declaration below the property, which carries the annotations itself (see PropertyRenderer).
        value.backingFieldSymbol?.takeIf { !it.isNotDefault }?.let { backingField ->
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
    context(session: KaSession, context: KaRenderingContext, output: KaRenderingOutput)
    override fun render(value: KaPropertySymbol, next: () -> Unit): Boolean {
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
        render(value to null, KaPiece.Annotations)
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
        render(value to null, KaPiece.Annotations)
        render(value, KaPiece.SymbolModifiers)

        if (value.isStatic) {
            // `static` is a Java modifier and has no Kotlin keyword token, so it is not covered by `AllowedKeywords` or `Modifiers`.
            output.append("static", KaTextAttribute.Keyword).space()
        }

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
        render(value to null, KaPiece.Annotations)
        render(value, KaPiece.SymbolModifiers)
        keyword(KtTokens.FIELD_KEYWORD, trailingSpace = false)
        output.append(": ", KaTextAttribute.Punctuation)
        render(value.returnType, KaPiece.Type)

        return true
    }
}
