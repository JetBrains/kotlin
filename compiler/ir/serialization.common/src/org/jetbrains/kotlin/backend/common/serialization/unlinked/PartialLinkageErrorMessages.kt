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
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageUtils.DeclarationId.Companion.declarationId
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageUtils.UNKNOWN_NAME
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageUtils.guessName
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.util.IdSignature.*
import org.jetbrains.kotlin.name.SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
import org.jetbrains.kotlin.backend.common.serialization.unlinked.PartialLinkageUtils.Module as PLModule

internal fun PartialLinkageCase.renderLinkageError(): String = buildString {
    when (this@renderLinkageError) {
        is UnusableClassifier -> unusableClassifier(cause, CauseRendering.Standalone, printIntermediateCause = false)
        is MissingDeclaration -> noDeclarationForSymbol(missingDeclarationSymbol)

        is DeclarationWithUnusableClassifier -> declarationWithUnusableClassifier(declarationSymbol, cause, forExpression = false)
        is ExpressionWithUnusableClassifier -> expressionWithUnusableClassifier(expression, cause)
        is ExpressionWithMissingDeclaration -> expression(expression) { noDeclarationForSymbol(missingDeclarationSymbol) }

        is ExpressionHasDeclarationWithUnusableClassifier -> expression(expression) {
            declarationWithUnusableClassifier(referencedDeclarationSymbol, cause, forExpression = true)
        }

        is ExpressionHasWrongTypeOfDeclaration -> expression(expression) {
            wrongTypeOfDeclaration(actualDeclarationSymbol, expectedDeclarationDescription)
        }

        is ExpressionDispatchReceiverMismatch -> expression(expression) {
            dispatchReceiverMismatch(expression.symbol, excessiveDispatchReceiver)
        }

        is ExpressionHasInaccessibleDeclaration -> expression(expression) {
            inaccessibleDeclaration(referencedDeclarationSymbol, declaringModule, useSiteModule)
        }

        is UnimplementedAbstractCallable -> unimplementedAbstractCallable(callable)
    }
}

private enum class DeclarationKind(val displayName: String) {
    CLASS("class"),
    INNER_CLASS("inner class"),
    DATA_CLASS("data class"),
    VALUE_CLASS("value class"),
    INTERFACE("interface"),
    FUN_INTERFACE("fun interface"),
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
                owner.isData -> DATA_CLASS
                owner.isAnonymousObject -> ANONYMOUS_OBJECT
                owner.isInner -> INNER_CLASS
                owner.isValue -> VALUE_CLASS
                else -> CLASS
            }
            ClassKind.INTERFACE -> if (owner.isFun) FUN_INTERFACE else INTERFACE
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

private data class Expression(val kind: ExpressionKind, val referencedDeclarationKind: DeclarationKind?)

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
        // First, try to guess name by the signature. This is the most reliable way, especially when the declaration itself is missing
        // and the symbol owner is just an IR stub, which is always a direct child of the auxiliary package fragment.
        ?.let { signature ->
            val nameSegmentsToPickUp = when {
                signature is AccessorSignature -> 2 // property_name.accessor_name
                this is IrConstructorSymbol -> if (owner.parent.isCompanionWithDefaultName()) 3 else 2 // class_name.<init> or class_name.Companion.<init>
                this is IrEnumEntrySymbol -> 2 // enum_class_name.entry_name
                owner.isCompanionWithDefaultName() -> 2 // class_name.Companion
                else -> 1
            }
            signature.guessName(nameSegmentsToPickUp)
        }
        ?: (owner as? IrDeclarationWithName)
            // Lazy IR may not have signatures. Let's try to extract name from the declaration itself.
            ?.let { owner ->
                when (owner) {
                    is IrSimpleFunction -> listOfNotNull(owner.correspondingPropertySymbol?.owner?.name, owner.name)
                    is IrConstructor -> {
                        val parent = owner.parentClassOrNull
                        when {
                            parent == null || parent.isAnonymousObject -> listOf(owner.name)
                            parent.isCompanionWithDefaultName() -> listOfNotNull(parent.parentClassOrNull?.name, parent.name, owner.name)
                            else -> listOf(parent.name, owner.name)
                        }
                    }
                    is IrEnumEntry -> listOfNotNull(owner.parentClassOrNull?.name, owner.name)
                    else -> listOf(owner.name)
                }.joinToString(".")
            }
}

private fun Appendable.signature(symbol: IrSymbol): Appendable {
    var file: String? = null

    val symbolRepresentation = symbol.signature?.render()
        ?: symbol.privateSignature?.let {
            // Try to extract symbol name from private signature if no public signature is available.
            // This could happen during visiting local IR entities declared inside function body.
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
        ?: (symbol.owner as? IrDeclarationWithName)?.let { lazyIrDeclaration ->
            // Lazy IR declaration might not have any signature at all. So let's print anything helpful at least.
            lazyIrDeclaration.declarationId?.let { "$it|?" /* We don't know the exact hash and mask to print them here. */ }
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

private fun Appendable.module(module: PLModule): Appendable =
    append("module ").append(module.name)

private sealed interface CauseRendering {
    // <subject> <reason>
    object Standalone : CauseRendering

    // <object> uses <subject>. <subject> <reason>
    // or
    // <object> uses <root subject> via <direct subject>. <root subject> <reason>
    sealed interface UsedFromSomewhere : CauseRendering {
        val objectText: String
        val objectSymbol: IrSymbol?
    }

    // <object> := <declaration>
    class UsedFromDeclaration(override val objectText: String, override val objectSymbol: IrSymbol) : UsedFromSomewhere

    // <object> := <expression>
    object UsedFromExpression : UsedFromSomewhere {
        override val objectText = "Expression"
        override val objectSymbol = null
    }
}

private fun Appendable.unusableClassifier(
    cause: Unusable,
    rendering: CauseRendering,
    printIntermediateCause: Boolean
): Appendable {
    val (rootCause: Unusable.CanBeRootCause, intermediateCause: Unusable.DueToOtherClassifier?) = when (cause) {
        is Unusable.CanBeRootCause -> cause to null
        is Unusable.DueToOtherClassifier -> cause.rootCause to cause
    }

    // some shortcuts:
    fun CauseRendering.UsedFromSomewhere.`object`() = append(objectText)
    fun CauseRendering.UsedFromSomewhere.objectUses() = `object`().append(" uses ")
    fun Appendable.subject(capitalized: Boolean) = declarationKindName(rootCause.symbol, capitalized)
    fun Appendable.subjectKind(capitalized: Boolean) = declarationKind(rootCause.symbol, capitalized)
    fun Appendable.via() = if (printIntermediateCause && intermediateCause != null)
        append(" (via ").declarationKindName(intermediateCause.symbol, capitalized = false).append(")") else this

    when (rootCause) {
        // Case: Missing class.
        is Unusable.MissingClassifier -> when (rendering) {
            CauseRendering.Standalone -> noDeclarationForSymbol(rootCause.symbol)

            is CauseRendering.UsedFromSomewhere -> with(rendering) {
                objectUses().append("unlinked ").subjectKind(capitalized = false).append(" symbol ").signature(rootCause.symbol).via()
            }
        }

        // Case: Invalid inheritance.
        is Unusable.InvalidInheritance -> {
            when {
                // Subcase: An error in constructor delegation.
                rootCause.unexpectedSuperClassConstructorSymbol != null -> {
                    when (rendering) {
                        CauseRendering.Standalone -> subject(capitalized = true).append(" has invalid constructor delegation:")

                        is CauseRendering.UsedFromSomewhere -> with(rendering) {
                            when {
                                objectSymbol == rootCause.symbol -> {
                                    // object == class itself
                                    `object`().append(" has invalid constructor delegation:")
                                }
                                (objectSymbol as? IrConstructorSymbol)?.owner?.parentClassOrNull?.symbol == rootCause.symbol -> {
                                    // object == class constructor
                                    `object`().append(" has invalid delegation:")
                                }
                                else -> objectUses().subject(capitalized = false).via().append(" that has invalid constructor delegation:")
                            }
                        }
                    }

                    // remainder (explanation):
                    append(" A call of a constructor of direct super class ")
                        .declarationName(rootCause.superClassSymbols.single()).append(" is expected instead of ")
                        .declarationName(rootCause.unexpectedSuperClassConstructorSymbol)
                }

                // Subcase: Invalid super class.
                rootCause.superClassSymbols.size == 1 -> {
                    fun Appendable.inheritsFromSuperClass(): Appendable {
                        val superClassSymbol = rootCause.superClassSymbols.single()
                        if (superClassSymbol.owner.modality == Modality.FINAL)
                            append(" inherits from final ")
                        else
                            append(" has illegal inheritance from ")
                        return declarationKindName(superClassSymbol, capitalized = false)
                    }

                    when (rendering) {
                        CauseRendering.Standalone -> subject(capitalized = true).inheritsFromSuperClass()

                        is CauseRendering.UsedFromSomewhere -> with(rendering) {
                            if (objectSymbol == rootCause.symbol) {
                                // 'rootCause.symbol' is already mentioned in `rendering.objectText`, so use shorter message
                                `object`().inheritsFromSuperClass()
                            } else {
                                objectUses().subject(capitalized = false).via().append(" that").inheritsFromSuperClass()
                            }
                        }
                    }
                }

                // Subcase: Inheritance from multiple classes (i.e. non-interfaces).
                else -> {
                    fun Appendable.inheritsFromMultipleClasses(): Appendable {
                        val renderedSuperClasses = ArrayList<String>(rootCause.superClassSymbols.size)
                        rootCause.superClassSymbols.mapTo(renderedSuperClasses) { buildString { declarationName(it) } }
                        renderedSuperClasses.sort()

                        append(" simultaneously inherits from ").append(renderedSuperClasses.size.toString()).append(" classes: ")
                        renderedSuperClasses.joinTo(this)

                        return this
                    }

                    when (rendering) {
                        CauseRendering.Standalone -> subject(capitalized = true).inheritsFromMultipleClasses()

                        is CauseRendering.UsedFromSomewhere -> with(rendering) {
                            if (objectSymbol == rootCause.symbol) {
                                // 'rootCause.symbol' is already mentioned in `rendering.objectText`, so use shorter message
                                `object`().inheritsFromMultipleClasses()
                            } else {
                                objectUses().subject(capitalized = false).via().append(" that").inheritsFromMultipleClasses()
                            }
                        }
                    }
                }
            }
        }

        // Case: Annotation class that has unacceptable type in a parameter.
        is Unusable.AnnotationWithUnacceptableParameter -> {
            fun Appendable.unacceptableClassifier(): Appendable = append("non-annotation ")
                .declarationKindName(rootCause.unacceptableClassifierSymbol, capitalized = false).append(" as a parameter")

            when (rendering) {
                CauseRendering.Standalone -> subject(capitalized = true).append(" has ").unacceptableClassifier()

                is CauseRendering.UsedFromSomewhere -> with(rendering) {
                    if (objectSymbol == rootCause.symbol) {
                        // 'rootCause.symbol' is already mentioned in `rendering.objectText`, so use shorter message
                        objectUses().unacceptableClassifier().via()
                    } else
                        objectUses().subject(capitalized = false).via().append(" that has ").unacceptableClassifier()
                }
            }
        }
    }

    return this
}

private fun Appendable.noDeclarationForSymbol(symbol: IrSymbol): Appendable =
    append("No ").declarationKind(symbol, capitalized = false).append(" found for symbol ").signature(symbol)

private fun Appendable.declarationWithUnusableClassifier(
    declarationSymbol: IrSymbol,
    cause: Unusable,
    forExpression: Boolean
): Appendable {
    val functionDeclaration = declarationSymbol.owner as? IrFunction

    val functionIsUnusableDueToContainingClass = if (functionDeclaration != null) {
        val rootCause = (cause as? Unusable.DueToOtherClassifier)?.rootCause ?: cause
        // If the root cause is InvalidInheritance due to invalid constructor delegation, then strictly speaking it's the problem
        // of a particular constructor than the containing class. And this should be reflected properly in the error message.
        (rootCause !is Unusable.InvalidInheritance || rootCause.unexpectedSuperClassConstructorSymbol == null)
                && (functionDeclaration.parent as? IrClass)?.symbol == cause.symbol
    } else false

    // The user (object) of the unusable classifier. In case the current declaration is a function with its own parent class being
    // the dispatch receiver, the class is the "object".
    val objectSymbol = if (functionIsUnusableDueToContainingClass) cause.symbol else declarationSymbol

    val objectDescription = buildString {
        if (functionIsUnusableDueToContainingClass) {
            // Callable member is unusable due to unusable dispatch receiver.
            val functionIsConstructor = functionDeclaration is IrConstructor

            if (forExpression) {
                if (functionIsConstructor)
                    declarationKindName(objectSymbol, capitalized = true) // "Class 'Foo'"
                else
                    append("Dispatch receiver ").declarationKindName(objectSymbol, capitalized = false) // "Dispatch receiver class 'Foo'"
            } else {
                declarationKindName(objectSymbol, capitalized = true) // "Class 'Foo'"
                    .append(if (functionIsConstructor) " created by " else ", the dispatch receiver of ") // ", the dispatch receiver of"
                    .declarationKindName(declarationSymbol, capitalized = false) // "function 'foo'"
            }
        } else {
            if (forExpression)
                declarationKind(objectSymbol, capitalized = true) // "Function"
            else
                declarationKindName(objectSymbol, capitalized = true) // "Function 'foo'"
        }
    }

    return unusableClassifier(
        cause,
        CauseRendering.UsedFromDeclaration(objectDescription, objectSymbol),
        printIntermediateCause = !functionIsUnusableDueToContainingClass
    )
}

private fun StringBuilder.expressionWithUnusableClassifier(
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

    unusableClassifier(cause, CauseRendering.UsedFromExpression, printIntermediateCause)
}

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

private fun Appendable.wrongTypeOfDeclaration(actualDeclarationSymbol: IrSymbol, expectedDeclarationDescription: String): Appendable =
    declarationNameIsKind(actualDeclarationSymbol).append(" while ").append(expectedDeclarationDescription).append(" is expected")

private fun Appendable.dispatchReceiverMismatch(
    invokedFunctionSymbol: IrFunctionSymbol,
    excessiveDispatchReceiver: Boolean
): Appendable {
    return if (excessiveDispatchReceiver) {
        append("The call site provides excessive dispatch receiver parameter 'this' that is not needed for the ")
        declarationKind(invokedFunctionSymbol, capitalized = false)
    } else {
        append("The call site does not provide a dispatch receiver parameter 'this' that the ")
        declarationKind(invokedFunctionSymbol, capitalized = false)
        append(" requires")
    }
}

private fun Appendable.inaccessibleDeclaration(
    referencedDeclarationSymbol: IrSymbol,
    declaringModule: PLModule,
    useSiteModule: PLModule
): Appendable = append("Private ").declarationKind(referencedDeclarationSymbol, capitalized = false)
    .append(" declared in ").module(declaringModule).append(" can not be accessed in ").module(useSiteModule)

private fun Appendable.unimplementedAbstractCallable(callable: IrOverridableDeclaration<*>): Appendable =
    append("Abstract ").declarationKindName(callable.symbol, capitalized = false)
        .append(" is not implemented in non-abstract ").declarationKindName(callable.parentAsClass.symbol, capitalized = false)

private fun Appendable.appendCapitalized(text: String, capitalized: Boolean): Appendable {
    if (capitalized && text.isNotEmpty()) {
        val firstChar = text[0]
        if (firstChar.isLowerCase())
            return append(firstChar.uppercaseChar()).append(text.substring(1))
    }

    return append(text)
}
