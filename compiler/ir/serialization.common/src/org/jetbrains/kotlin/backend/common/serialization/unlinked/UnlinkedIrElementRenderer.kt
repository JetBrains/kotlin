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
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.util.nameForIrSerialization
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

// TODO: Consider getting rid of this class when new self-descriptive signatures are implemented.
internal class UnlinkedIrElementRenderer(private val getLocalClassName: (IrAttributeContainer) -> String?) {
    fun renderPath(element: IrElement): String =
        Worker(renderExplanation = false, unlinkedSymbols = emptyList()).computeAndRender(element)

    fun renderErrorMessage(element: IrElement, unlinkedSymbols: Collection<IrSymbol>): String =
        Worker(renderExplanation = true, unlinkedSymbols).computeAndRender(element)

    private inner class Worker(
        private val renderExplanation: Boolean,
        private val unlinkedSymbols: Collection<IrSymbol>
    ) {
        private var expressionKind: ExpressionKind? = null
        private val declarations: MutableList<Declaration> = mutableListOf()

        fun computeAndRender(element: IrElement): String {
            compute(element)
            compress()
            return render()
        }

        private fun compute(element: IrElement) {
            if (element is IrClass) {
                val className =
                    getLocalClassName(element) // This is a local class (or lambda, or anonymous object, etc) with already known name.
                        ?: element.fqNameForIrSerialization.asString() // For the rest of classes its always possible to compute stable FQNs.
                declarations += Declaration(element.declarationKind, className)
                return
            }

            when (element) {
                is IrDeclaration -> {
                    declarations += when (element) {
                        is IrSimpleFunction -> computeDeclarationMaybeUnderProperty(element, element.correspondingPropertySymbol)
                        is IrField -> computeDeclarationMaybeUnderProperty(element, element.correspondingPropertySymbol)
                        else -> computeDeclarationMaybeUnderProperty(element, correspondingPropertySymbol = null)
                    }

                    (element.parent as? IrDeclaration)?.let(::compute) // Recurse.
                }
                is IrExpression -> {
                    val expression = element.expression
                    val referencedDeclarationSymbol = when (element) {
                        is IrDeclarationReference -> element.symbol
                        is IrInstanceInitializerCall -> element.classSymbol
                        else -> null
                    }
                    val referencedDeclaration = referencedDeclarationSymbol?.boundOwnerDeclarationOrNull

                    expressionKind = expression.kind
                    if (referencedDeclaration == null && expression.referencedDeclarationKind != null) {
                        // If it's impossible to obtain references declaration (because the symbol of this declaration is unbound),
                        // but the declaration itself is supposed to be, then do best effort and try to output the simple name of
                        // the declaration at least.
                        declarations += Declaration(expression.referencedDeclarationKind, referencedDeclarationSymbol?.guessFullName)
                    }

                    referencedDeclaration?.let(::compute) // Recurse over referenced declaration.
                }
                else -> error("Unexpected type of IR element: ${this::class.java}, $this")
            }
        }

        // For top-level declarations computes the FQN including package name.
        // For non-top-level declarations computes the FQN consisting only of short name.
        private fun computeMaybeFQN(declaration: IrDeclaration, shortName: Name?): FqName {
            val effectiveName = shortName ?: UNKNOWN_NAME
            val basePackageFQN = (declaration.parent as? IrPackageFragment)?.fqName ?: FqName.ROOT
            return basePackageFQN.child(effectiveName)
        }

        // Special handling for property accessors and property fields:
        private fun computeDeclarationMaybeUnderProperty(
            declaration: IrDeclaration,
            correspondingPropertySymbol: IrPropertySymbol?
        ): Declaration {
            val declarationShortName = declaration.nameForIrSerialization

            val declarationName = if (correspondingPropertySymbol != null) {
                // This is a declaration under a property.
                val propertyShortName = correspondingPropertySymbol.guessShortName

                if (declaration is IrField && propertyShortName == declarationShortName) {
                    // It's a typical situation when field has exactly the same name as its property.
                    computeMaybeFQN(declaration, propertyShortName)
                } else
                    computeMaybeFQN(declaration, propertyShortName).child(declarationShortName)
            } else {
                // This is a declaration NOT under a property.
                computeMaybeFQN(declaration, declarationShortName)
            }

            return Declaration(declaration.declarationKind, declarationName.asString())
        }

        private fun compress() {
            if (declarations.size < 2)
                return

            val last = declarations.last()
            if (!last.kind.isClass || last.name.isNullOrEmpty())
                return

            val penultimate = declarations[declarations.lastIndex - 1]
            if (penultimate.name.isNullOrEmpty())
                return

            declarations.removeLast()
            declarations[declarations.lastIndex] = penultimate.copy(name = "${last.name}.${penultimate.name}")
        }

        private fun render(): String = buildString {
            expressionKind?.displayName?.let(::append)

            declarations.forEachIndexed { index, declaration ->
                appendWhitespaceIfNotEmpty()
                if (index > 0) append("declared in ")
                append(declaration.kind).appendWithWhitespace(declaration.name)
            }

            if (renderExplanation) {
                val verb3rdForm = expressionKind?.verb3rdForm
                if (verb3rdForm?.isNotEmpty() == true) {
                    append(" can not be ").append(verb3rdForm).append(" because it")
                }
                append(" uses unlinked symbols")
                if (unlinkedSymbols.isNotEmpty()) {
                    unlinkedSymbols.joinTo(this, prefix = ": ") { it.anySignature?.render() ?: "<unknown symbol>" }
                }
            }
        }
    }
}

private data class Declaration(val kind: DeclarationKind, val name: String?)

private enum class DeclarationKind(val isClass: Boolean = false, private val displayName: () -> String) {
    CLASS(isClass = true, displayName = { ClassKind.CLASS.codeRepresentation!! }),
    INTERFACE(isClass = true, displayName = { ClassKind.INTERFACE.codeRepresentation!! }),
    ENUM_CLASS(isClass = true, displayName = { ClassKind.ENUM_CLASS.codeRepresentation!! }),
    ENUM_ENTRY(isClass = true, displayName = { "enum entry" }),
    ANNOTATION_CLASS(isClass = true, displayName = { ClassKind.ANNOTATION_CLASS.codeRepresentation!! }),
    OBJECT(isClass = true, displayName = { ClassKind.OBJECT.codeRepresentation!! }),
    MUTABLE_VARIABLE(displayName = { "var" }),
    IMMUTABLE_VARIABLE(displayName = { "val" }),
    VALUE_PARAMETER(displayName = { "value parameter" }),
    FIELD(displayName = { "field" }),
    FIELD_OF_PROPERTY(displayName = { "backing field of property" }),
    PROPERTY(displayName = { "property" }),
    PROPERTY_ACCESSOR(displayName = { "property accessor" }),
    FUNCTION(displayName = { "function" }),
    CONSTRUCTOR(displayName = { "constructor" }),
    OTHER_DECLARATION(displayName = { "declaration" });

    override fun toString() = displayName()
}

private val IrDeclaration.declarationKind: DeclarationKind
    get() = when (this) {
        is IrClass -> when (kind) {
            ClassKind.CLASS -> CLASS
            ClassKind.INTERFACE -> INTERFACE
            ClassKind.ENUM_CLASS -> ENUM_CLASS
            ClassKind.ENUM_ENTRY -> ENUM_ENTRY
            ClassKind.ANNOTATION_CLASS -> ANNOTATION_CLASS
            ClassKind.OBJECT -> OBJECT
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

private val IrSymbol.guessShortName: Name?
    get() = boundOwnerDeclarationOrNull?.nameForIrSerialization
        ?: anySignature?.guessShortName

private val IdSignature.guessShortName: Name?
    get() = when (this) {
        is CommonSignature -> Name.guessByFirstCharacter(shortName)
        is CompositeSignature -> inner.guessShortName
        is AccessorSignature -> accessorSignature.guessShortName
        else -> null
    }

private val IrSymbol.guessFullName: String?
    get() = (boundOwnerDeclarationOrNull as? IrDeclarationParent)?.fqNameForIrSerialization?.asString()
        ?: anySignature?.guessFullName

private val IdSignature.guessFullName: String?
    get() = when (this) {
        is CommonSignature -> if (packageFqName.isNotEmpty()) "$packageFqName.$declarationFqName" else declarationFqName
        is CompositeSignature -> if (container is FileSignature) inner.guessFullName else null
        is AccessorSignature -> accessorSignature.guessFullName
        else -> null
    }

private val IrSymbol.anySignature: IdSignature?
    get() = signature ?: privateSignature

private val UNKNOWN_NAME = Name.special("<unknown name>")

private fun StringBuilder.appendWithWhitespace(text: String?): StringBuilder {
    if (!text.isNullOrEmpty()) appendWhitespaceIfNotEmpty().append(text)
    return this
}

private fun StringBuilder.appendWhitespaceIfNotEmpty(): StringBuilder {
    if (isNotEmpty()) append(" ")
    return this
}
