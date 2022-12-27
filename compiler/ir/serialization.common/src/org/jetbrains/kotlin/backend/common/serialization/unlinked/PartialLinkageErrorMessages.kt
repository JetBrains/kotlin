/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common.serialization.unlinked

import com.intellij.util.PathUtil
import org.jetbrains.kotlin.backend.common.serialization.unlinked.DeclarationKind.*
import org.jetbrains.kotlin.backend.common.serialization.unlinked.ExploredClassifier.Unusable
import org.jetbrains.kotlin.backend.common.serialization.unlinked.ExpressionKind.*
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageCase.*
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageUtils.UNKNOWN_NAME
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageUtils.guessName
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.IdSignature.*
import org.jetbrains.kotlin.ir.util.isAnonymousObject
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.name.SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageUtils.Module as PLModule

// TODO: Simplify and enhance PL error messages when new self-descriptive signatures are implemented.
internal fun PartialLinkageCase.renderErrorMessage(): String = buildString {
    when (this@renderErrorMessage) {
        is MissingDeclaration -> noDeclarationForSymbol(missingDeclarationSymbol)
        is MissingEnclosingClass -> noEnclosingClass(orphanedClassSymbol)

        is DeclarationUsesPartiallyLinkedClassifier ->
            declarationWithPartiallyLinkedClassifier(declarationSymbol, cause, forExpression = false)

        is UnimplementedAbstractCallable -> unimplementedAbstractCallable(callable)
        is ExpressionUsesMissingDeclaration -> expression(expression) { noDeclarationForSymbol(missingDeclarationSymbol) }
        is ExpressionUsesPartiallyLinkedClassifier -> expressionWithPartiallyLinkedClassifier(expression, cause)

        is ExpressionUsesDeclarationThatUsesPartiallyLinkedClassifier -> expression(expression) {
            declarationWithPartiallyLinkedClassifier(referencedDeclarationSymbol, cause, forExpression = true)
        }

        is ExpressionUsesWrongTypeOfDeclaration -> expression(expression) {
            wrongTypeOfDeclaration(actualDeclarationSymbol, expectedDeclarationDescription)
        }

        is ExpressionsUsesInaccessibleDeclaration -> expression(expression) {
            inaccessibleDeclaration(referencedDeclarationSymbol, declaringModule, useSiteModule)
        }
    }
}

private enum class DeclarationKind(val displayName: String) {
    CLASS("class"),
    INNER_CLASS("inner class"),
    INTERFACE("interface"),
    ENUM_CLASS("enum class"),
    ENUM_ENTRY("enum entry"),
    ENUM_ENTRY_CLASS("enum entry class"),
    ANNOTATION_CLASS("annotation class"),
    OBJECT("object"),
    ANONYMOUS_OBJECT("anonymous object"),
    COMPANION_OBJECT("companion object"),
    VARIABLE("variable"),
    VALUE_PARAMETER("value parameter"),
    FIELD("field"),
    FIELD_OF_PROPERTY("backing field of property"),
    PROPERTY("property"),
    PROPERTY_ACCESSOR("property accessor"),
    FUNCTION("function"),
    CONSTRUCTOR("constructor"),
    OTHER_DECLARATION("declaration");
}

private val IrSymbol.declarationKind: DeclarationKind
    get() = when (this) {
        is IrClassSymbol -> when (owner.kind) {
            ClassKind.CLASS -> when {
                owner.isAnonymousObject -> ANONYMOUS_OBJECT
                owner.isInner -> INNER_CLASS
                else -> CLASS
            }
            ClassKind.INTERFACE -> INTERFACE
            ClassKind.ENUM_CLASS -> ENUM_CLASS
            ClassKind.ENUM_ENTRY -> ENUM_ENTRY_CLASS
            ClassKind.ANNOTATION_CLASS -> ANNOTATION_CLASS
            ClassKind.OBJECT -> if (owner.isCompanion) COMPANION_OBJECT else OBJECT
        }
        is IrEnumEntrySymbol -> ENUM_ENTRY
        is IrVariableSymbol -> VARIABLE
        is IrValueParameterSymbol -> VALUE_PARAMETER
        is IrFieldSymbol -> if (owner.correspondingPropertySymbol != null) FIELD_OF_PROPERTY else FIELD
        is IrPropertySymbol -> PROPERTY
        is IrSimpleFunctionSymbol -> if (owner.correspondingPropertySymbol != null || signature is AccessorSignature) PROPERTY_ACCESSOR else FUNCTION
        is IrConstructorSymbol -> CONSTRUCTOR
        else -> OTHER_DECLARATION
    }

private data class Expression(val kind: ExpressionKind, val referencedDeclarationKind: DeclarationKind?)

private enum class ExpressionKind(val prefix: String?, val postfix: String?) {
    REFERENCE("Reference to", "can not be evaluated"),
    CALLING(null, "can not be called"),
    CALLING_INSTANCE_INITIALIZER("Instance initializer of", "can not be called"),
    READING("Can not read value from", null),
    WRITING("Can not write value to", null),
    GETTING_INSTANCE("Can not get instance of", null),
    TYPE_OPERATOR("Type operator expression", "can not be evaluated"),
    ANONYMOUS_OBJECT_LITERAL("Anonymous object literal", "can not be evaluated"),
    OTHER_EXPRESSION("Expression", "can not be evaluated")
}

// More can be added for verbosity in the future.
private val IrExpression.expression: Expression
    get() = when (this) {
        is IrDeclarationReference -> when (this) {
            is IrFunctionReference -> Expression(REFERENCE, symbol.declarationKind)
            is IrPropertyReference,
            is IrLocalDelegatedPropertyReference -> Expression(REFERENCE, PROPERTY)
            is IrCall -> Expression(CALLING, symbol.declarationKind)
            is IrConstructorCall,
            is IrEnumConstructorCall,
            is IrDelegatingConstructorCall -> Expression(CALLING, CONSTRUCTOR)
            is IrClassReference -> Expression(REFERENCE, symbol.declarationKind)
            is IrGetField -> Expression(READING, symbol.declarationKind)
            is IrSetField -> Expression(WRITING, symbol.declarationKind)
            is IrGetValue -> Expression(READING, symbol.declarationKind)
            is IrSetValue -> Expression(WRITING, symbol.declarationKind)
            is IrGetSingletonValue -> Expression(GETTING_INSTANCE, symbol.declarationKind)
            else -> Expression(REFERENCE, OTHER_DECLARATION)
        }
        is IrInstanceInitializerCall -> Expression(CALLING_INSTANCE_INITIALIZER, classSymbol.declarationKind)
        is IrTypeOperatorCall -> Expression(TYPE_OPERATOR, null)
        else -> {
            if (this is IrBlock && origin == IrStatementOrigin.OBJECT_LITERAL)
                Expression(ANONYMOUS_OBJECT_LITERAL, null)
            else
                Expression(OTHER_EXPRESSION, null)
        }
    }

private fun IrSymbol.guessName(): String? {
    fun IrElement.isCompanionWithDefaultName() = this is IrClass && isCompanion && name == DEFAULT_NAME_FOR_COMPANION_OBJECT

    return signature
        ?.let { effectiveSignature ->
            val nameSegmentsToPickUp = when {
                effectiveSignature is AccessorSignature -> 2 // property_name.accessor_name
                this is IrConstructorSymbol -> if (owner.parent.isCompanionWithDefaultName()) 3 else 2 // class_name.<init> or class_name.Companion.<init>
                this is IrEnumEntrySymbol -> 2 // enum_class_name.entry_name
                owner.isCompanionWithDefaultName() -> 2 // class_name.Companion
                else -> 1
            }
            effectiveSignature.guessName(nameSegmentsToPickUp)
        }
        ?: (owner as? IrDeclarationWithName)?.name?.asString()
}

private fun Appendable.signature(symbol: IrSymbol): Appendable {
    var file: String? = null
    val symbolRepresentation = symbol.signature?.render()
        ?: symbol.privateSignature?.let {
            when (it) {
                is FileSignature -> null // Avoid printing FileSignature.
                is CompositeSignature -> {
                    // Avoid printing full paths from FileSignature.
                    val container = it.container
                    if (container is FileSignature) {
                        file = PathUtil.getFileName(container.fileName).takeIf(String::isNotEmpty) ?: UNKNOWN_FILE
                        it.inner.render()
                    } else it.render()
                }
                else -> it.render()
            }
        }
        ?: UNKNOWN_SYMBOL

    append('\'').append(symbolRepresentation).append('\'')
    if (file != null) append(" declared in file ").append(file)
    return this
}

private const val UNKNOWN_SYMBOL = "<unknown symbol>"
private const val UNKNOWN_FILE = "<unknown file>"

private fun Appendable.declarationName(symbol: IrSymbol): Appendable =
    append('\'').append(symbol.guessName() ?: UNKNOWN_NAME.asString()).append('\'')

private fun Appendable.declarationKind(symbol: IrSymbol, capitalized: Boolean): Appendable =
    appendCapitalized(symbol.declarationKind.displayName, capitalized)

private fun Appendable.declarationKindName(symbol: IrSymbol, capitalized: Boolean): Appendable {
    val declarationKind = symbol.declarationKind
    appendCapitalized(declarationKind.displayName, capitalized)
    if (declarationKind != ANONYMOUS_OBJECT) append(" ").declarationName(symbol)
    return this
}

private fun Appendable.declarationNameIsKind(symbol: IrSymbol): Appendable =
    declarationName(symbol).append(" is ").declarationKind(symbol, capitalized = false)

private fun StringBuilder.expression(expression: IrExpression, continuation: (ExpressionKind) -> Appendable): Appendable {
    val (expressionKind, referencedDeclarationKind) = expression.expression

    // Prefix may be null. But when it's not null, it is always capitalized.
    val hasPrefix = expressionKind.prefix != null
    if (hasPrefix) append(expressionKind.prefix)

    if (referencedDeclarationKind != null) {
        if (hasPrefix) append(" ")

        when (expression) {
            is IrGetSingletonValue -> appendCapitalized("singleton", capitalized = !hasPrefix)
                .append(" ").declarationName(expression.symbol)
            is IrDeclarationReference -> declarationKindName(expression.symbol, capitalized = !hasPrefix)
            is IrInstanceInitializerCall -> declarationKindName(expression.classSymbol, capitalized = !hasPrefix)
            else -> appendCapitalized(referencedDeclarationKind.displayName, capitalized = !hasPrefix)
        }
    }

    expressionKind.postfix?.let { postfix -> append(" ").append(postfix) }
    append(": ")

    return continuation(expressionKind)
}

private fun Appendable.cause(cause: Unusable): Appendable =
    when (cause) {
        is Unusable.MissingClassifier -> unlinkedSymbol(cause)
        is Unusable.MissingEnclosingClass -> noEnclosingClass(cause)
        is Unusable.DueToOtherClassifier -> {
            when (val rootCause = cause.rootCause) {
                is Unusable.MissingClassifier -> unlinkedSymbol(rootCause, cause)
                is Unusable.MissingEnclosingClass -> noEnclosingClass(rootCause, cause)
            }
        }
    }

private fun Appendable.noDeclarationForSymbol(symbol: IrSymbol): Appendable =
    append("No ").declarationKind(symbol, capitalized = false).append(" found for symbol ").signature(symbol)

private fun Appendable.noEnclosingClass(symbol: IrClassSymbol): Appendable =
    declarationKindName(symbol, capitalized = true).append(" lacks enclosing class")

private fun Appendable.unlinkedSymbol(
    rootCause: Unusable.MissingClassifier,
    cause: Unusable.DueToOtherClassifier? = null
): Appendable {
    append("unlinked ").declarationKind(rootCause.symbol, capitalized = false).append(" symbol ").signature(rootCause.symbol)
    if (cause != null) through(cause)
    return this
}

private fun Appendable.noEnclosingClass(
    rootCause: Unusable.MissingEnclosingClass,
    cause: Unusable.DueToOtherClassifier? = null
): Appendable {
    declarationKindName(rootCause.symbol, capitalized = false)
    if (cause != null) through(cause)
    return append(". ").noEnclosingClass(rootCause.symbol)
}

private fun Appendable.through(cause: Unusable.DueToOtherClassifier): Appendable =
    append(" (through ").declarationKindName(cause.symbol, capitalized = false).append(")")

private fun Appendable.module(module: PLModule): Appendable =
    append("module ").append(module.name)

private fun Appendable.unimplementedAbstractCallable(callable: IrOverridableDeclaration<*>): Appendable =
    append("Abstract ").declarationKindName(callable.symbol, capitalized = false)
        .append(" is not implemented in non-abstract ").declarationKindName(callable.parentAsClass.symbol, capitalized = false)

private fun Appendable.wrongTypeOfDeclaration(actualDeclarationSymbol: IrSymbol, expectedDeclarationDescription: String): Appendable =
    declarationNameIsKind(actualDeclarationSymbol).append(" while ").append(expectedDeclarationDescription).append(" is expected")

private fun Appendable.declarationWithPartiallyLinkedClassifier(
    declarationSymbol: IrSymbol,
    cause: Unusable,
    forExpression: Boolean
): Appendable {
    val declaration = declarationSymbol.owner
    return if (declaration is IrFunction && declaration.parentClassOrNull?.symbol == cause.symbol) {
        // Callable member is unusable due to unusable dispatch receiver.
        if (forExpression) {
            if (declaration is IrConstructor)
                declarationKindName(cause.symbol, capitalized = true)
            else
                append("Dispatch receiver ").declarationKindName(cause.symbol, capitalized = true)
            append(" uses ").cause(cause.rootCause)
        } else {
            declarationKindName(cause.symbol, capitalized = true)
                .append(if (declaration is IrConstructor) " created by " else ", the dispatch receiver of ")
                .declarationKindName(declarationSymbol, capitalized = false).append(" uses ").cause(cause.rootCause)
        }
    } else {
        if (forExpression)
            declarationKind(declarationSymbol, capitalized = true)
        else
            declarationKindName(declarationSymbol, capitalized = true)
        append(" uses ").cause(cause)
    }
}

private fun StringBuilder.expressionWithPartiallyLinkedClassifier(
    expression: IrExpression,
    cause: Unusable
): Appendable = expression(expression) { expressionKind ->
    // Printing the intermediate cause may pollute certain types of error messages. Need to avoid it when possible.
    val printIntermediateCause = when {
        expression is IrGetSingletonValue -> when (val expressionSymbol = expression.symbol) {
            is IrEnumEntrySymbol -> (expressionSymbol.owner.parent as? IrClass)?.symbol != cause.symbol
            else -> expressionSymbol != cause.symbol
        }

        expressionKind == ANONYMOUS_OBJECT_LITERAL -> cause.symbol.declarationKind != ANONYMOUS_OBJECT

        else -> true
    }

    append("Expression uses ").cause(if (printIntermediateCause) cause else cause.rootCause)
}

private fun Appendable.inaccessibleDeclaration(
    referencedDeclarationSymbol: IrSymbol,
    declaringModule: PLModule,
    useSiteModule: PLModule
): Appendable = append("Private ").declarationKind(referencedDeclarationSymbol, capitalized = false)
    .append(" declared in ").module(declaringModule).append(" can not be accessed in ").module(useSiteModule)

private fun Appendable.appendCapitalized(text: String, capitalized: Boolean): Appendable {
    if (capitalized && text.isNotEmpty()) {
        val firstChar = text[0]
        if (firstChar.isLowerCase())
            return append(firstChar.uppercaseChar()).append(text.substring(1))
    }

    return append(text)
}

private val Unusable.rootCause get() = if (this is Unusable.DueToOtherClassifier) rootCause else this
