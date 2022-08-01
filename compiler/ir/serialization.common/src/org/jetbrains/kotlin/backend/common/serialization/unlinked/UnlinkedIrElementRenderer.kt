/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import org.jetbrains.kotlin.backend.common.serialization.unlinked.DeclarationKind.*
import org.jetbrains.kotlin.backend.common.serialization.unlinked.ExpressionKind.*
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature
import org.jetbrains.kotlin.ir.util.IdSignature.*
import org.jetbrains.kotlin.ir.util.isAnonymousObject
import org.jetbrains.kotlin.ir.util.nameForIrSerialization

// TODO: Consider getting rid of this class when new self-descriptive signatures are implemented.
internal object UnlinkedIrElementRenderer {
    fun StringBuilder.appendDeclaration(declaration: IrDeclaration): StringBuilder = appendDeclaration(
        declarationKind = declaration.declarationKind,
        declarationName = declaration.symbol.guessName()
    )

    private fun StringBuilder.appendDeclaration(declarationKind: DeclarationKind, declarationName: String?): StringBuilder {
        append(declarationKind)

        if (declarationKind != ANONYMOUS_OBJECT) {
            // This is a declaration NOT under a property.
            appendWithWhitespaceBefore(declarationName ?: UNKNOWN_NAME)
        }

        return this
    }

    fun renderError(element: IrElement, unlinkedSymbols: Collection<IrSymbol>): String = buildString {
        when (element) {
            is IrDeclaration -> appendDeclaration(element)
            is IrExpression -> {
                val (expressionKind, referencedDeclarationKind) = element.expression
                appendWithWhitespaceAfter(expressionKind.displayName)

                if (referencedDeclarationKind != null) {
                    val referencedDeclarationSymbol = when (element) {
                        is IrDeclarationReference -> element.symbol
                        is IrInstanceInitializerCall -> element.classSymbol
                        else -> null
                    }
                    val referencedDeclaration = referencedDeclarationSymbol?.boundOwnerDeclarationOrNull
                    if (referencedDeclaration == null) {
                        // If it's impossible to obtain referenced declaration because the symbol of this declaration is unbound,
                        // but the declaration itself is supposed to be, then do best effort and try to output the short name of
                        // the declaration at least.
                        appendDeclaration(referencedDeclarationKind, referencedDeclarationSymbol?.guessName())
                    } else {
                        appendDeclaration(referencedDeclaration)
                    }
                }

                append(" can not be ").append(expressionKind.verb3rdForm).append(" because it")
            }
            else -> error("Unexpected type of IR element: ${this::class.java}, $this")
        }

        append(" uses unlinked symbols")
        if (unlinkedSymbols.isNotEmpty()) {
            unlinkedSymbols.joinTo(this, prefix = ": ") { it.anySignature?.render() ?: UNKNOWN_SYMBOL }
        }
    }
}

private enum class DeclarationKind(private val displayName: String) {
    CLASS("class"),
    INTERFACE("interface"),
    ENUM_CLASS("enum class"),
    ENUM_ENTRY("enum entry"),
    ANNOTATION_CLASS("annotation class"),
    OBJECT("object"),
    ANONYMOUS_OBJECT("anonymous object"),
    COMPANION_OBJECT("companion object"),
    MUTABLE_VARIABLE("var"),
    IMMUTABLE_VARIABLE("val"),
    VALUE_PARAMETER("value parameter"),
    FIELD("field"),
    FIELD_OF_PROPERTY("backing field of property"),
    PROPERTY("property"),
    PROPERTY_ACCESSOR("property accessor"),
    FUNCTION("function"),
    CONSTRUCTOR("constructor"),
    OTHER_DECLARATION("declaration");

    override fun toString() = displayName
}

private val IrDeclaration.declarationKind: DeclarationKind
    get() = when (this) {
        is IrClass -> when (kind) {
            ClassKind.CLASS -> if (isAnonymousObject) ANONYMOUS_OBJECT else CLASS
            ClassKind.INTERFACE -> INTERFACE
            ClassKind.ENUM_CLASS -> ENUM_CLASS
            ClassKind.ENUM_ENTRY -> ENUM_ENTRY
            ClassKind.ANNOTATION_CLASS -> ANNOTATION_CLASS
            ClassKind.OBJECT -> if (isCompanion) COMPANION_OBJECT else OBJECT
        }
        is IrVariable -> if (isVar) MUTABLE_VARIABLE else IMMUTABLE_VARIABLE
        is IrValueParameter -> VALUE_PARAMETER
        is IrField -> if (correspondingPropertySymbol != null) FIELD_OF_PROPERTY else FIELD
        is IrProperty -> PROPERTY
        is IrSimpleFunction -> if (correspondingPropertySymbol != null) PROPERTY_ACCESSOR else FUNCTION
        is IrConstructor -> CONSTRUCTOR
        else -> OTHER_DECLARATION
    }

private val IrFunctionSymbol.functionDeclarationKind: DeclarationKind
    get() = when (this) {
        is IrConstructorSymbol -> CONSTRUCTOR
        is IrSimpleFunctionSymbol -> boundOwnerDeclarationOrNull?.declarationKind
            ?: if (anySignature is AccessorSignature) PROPERTY_ACCESSOR else FUNCTION
        else -> OTHER_DECLARATION // Something unexpected.
    }

private val IrFieldSymbol.fieldDeclarationKind: DeclarationKind
    get() = boundOwnerDeclarationOrNull?.declarationKind
        ?: FIELD // Fallback to simple field as it's impossible to make a guess by signature.

private val IrValueSymbol.valueDeclarationKind: DeclarationKind
    get() = when (this) {
        is IrValueParameterSymbol -> VALUE_PARAMETER
        is IrVariableSymbol -> boundOwnerDeclarationOrNull?.declarationKind
            ?: IMMUTABLE_VARIABLE // Fallback to immutable variable as it's impossible to make a guess by signature.
        else -> OTHER_DECLARATION // Something unexpected.
    }

private val IrSymbol.classDeclarationKind: DeclarationKind
    get() = when (this) {
        is IrClassSymbol -> boundOwnerDeclarationOrNull?.declarationKind
            ?: CLASS // Fallback to class as it's impossible to make a guess by signature.
        is IrEnumEntrySymbol -> ENUM_ENTRY
        else -> OTHER_DECLARATION // Something unexpected.
    }

private data class Expression(val kind: ExpressionKind, val referencedDeclarationKind: DeclarationKind?)

private enum class ExpressionKind(val displayName: String?, val verb3rdForm: String) {
    REFERENCE("reference to", "evaluated"),
    CALLING(null, "called"),
    CALLING_INSTANCE_INITIALIZER("instance initializer of", "called"),
    READING(null, "read"),
    WRITING(null, "written"),
    GETTING_INSTANCE(null, "gotten"),
    OTHER_EXPRESSION("expression", "evaluated")
}

// More can be added for verbosity in the future.
private val IrExpression.expression: Expression
    get() = when (this) {
        is IrDeclarationReference -> when (this) {
            is IrFunctionReference -> Expression(REFERENCE, symbol.functionDeclarationKind)
            is IrPropertyReference,
            is IrLocalDelegatedPropertyReference -> Expression(REFERENCE, PROPERTY)
            is IrCall -> Expression(CALLING, symbol.functionDeclarationKind)
            is IrConstructorCall,
            is IrEnumConstructorCall,
            is IrDelegatingConstructorCall -> Expression(CALLING, CONSTRUCTOR)
            is IrClassReference -> Expression(REFERENCE, symbol.classDeclarationKind)
            is IrGetField -> Expression(READING, symbol.fieldDeclarationKind)
            is IrSetField -> Expression(WRITING, symbol.fieldDeclarationKind)
            is IrGetValue -> Expression(READING, symbol.valueDeclarationKind)
            is IrSetValue -> Expression(WRITING, symbol.valueDeclarationKind)
            is IrGetSingletonValue -> Expression(GETTING_INSTANCE, symbol.classDeclarationKind)
            else -> Expression(REFERENCE, OTHER_DECLARATION)
        }
        is IrInstanceInitializerCall -> Expression(CALLING_INSTANCE_INITIALIZER, classSymbol.classDeclarationKind)
        else -> Expression(OTHER_EXPRESSION, null)
    }

private val IrSymbol.boundOwnerDeclarationOrNull: IrDeclaration?
    get() = if (isBound) owner as? IrDeclaration else null

private fun IrSymbol.guessName(): String? {
    fun IdSignature.guessNameBySignature(nameSegmentsToPickUp: Int): String? = when (this) {
        is CommonSignature -> nameSegments.takeLast(nameSegmentsToPickUp).joinToString(".")
        is CompositeSignature -> inner.guessNameBySignature(nameSegmentsToPickUp)
        is AccessorSignature -> accessorSignature.guessNameBySignature(nameSegmentsToPickUp)
        else -> null
    }

    return anySignature
        ?.let { effectiveSignature ->
            val nameSegmentsToPickUp = when {
                effectiveSignature is AccessorSignature -> 2 // property_name.accessor_name
                this is IrConstructorSymbol -> 2 // class_name.<init>
                else -> 1
            }
            effectiveSignature.guessNameBySignature(nameSegmentsToPickUp)
        }
        ?: boundOwnerDeclarationOrNull?.nameForIrSerialization?.asString()
}

private val IrSymbol.anySignature: IdSignature?
    get() = signature ?: privateSignature

private const val UNKNOWN_SYMBOL = "<unknown symbol>"
private const val UNKNOWN_NAME = "<unknown name>"

private fun StringBuilder.appendWithWhitespaceBefore(text: String): StringBuilder {
    if (text.isNotEmpty()) appendWhitespaceIfNotEmpty().append(text)
    return this
}

private fun StringBuilder.appendWithWhitespaceAfter(text: String?): StringBuilder {
    if (!text.isNullOrEmpty()) append(text).append(" ")
    return this
}

private fun StringBuilder.appendWhitespaceIfNotEmpty(): StringBuilder {
    if (isNotEmpty()) append(" ")
    return this
}
