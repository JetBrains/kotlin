/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// Impl classes from kotlin.library.abi.impl are necessary to instantiate parsed declarations
@file:OptIn(ExperimentalLibraryAbiReader::class)

package org.jetbrains.kotlin.library.abi.parser

import kotlin.text.dropLast
import org.jetbrains.kotlin.library.abi.AbiClassKind
import org.jetbrains.kotlin.library.abi.AbiCompoundName
import org.jetbrains.kotlin.library.abi.AbiModality
import org.jetbrains.kotlin.library.abi.AbiPropertyKind
import org.jetbrains.kotlin.library.abi.AbiQualifiedName
import org.jetbrains.kotlin.library.abi.AbiSignatureVersion
import org.jetbrains.kotlin.library.abi.AbiType
import org.jetbrains.kotlin.library.abi.AbiTypeArgument
import org.jetbrains.kotlin.library.abi.AbiTypeNullability
import org.jetbrains.kotlin.library.abi.AbiTypeParameter
import org.jetbrains.kotlin.library.abi.AbiValueParameter
import org.jetbrains.kotlin.library.abi.AbiValueParameterKind
import org.jetbrains.kotlin.library.abi.AbiVariance
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader
import org.jetbrains.kotlin.library.abi.impl.AbiTypeParameterImpl
import org.jetbrains.kotlin.library.abi.impl.AbiValueParameterImpl
import org.jetbrains.kotlin.library.abi.impl.ClassReferenceImpl
import org.jetbrains.kotlin.library.abi.impl.SimpleTypeImpl
import org.jetbrains.kotlin.library.abi.impl.StarProjectionImpl
import org.jetbrains.kotlin.library.abi.impl.TypeParameterReferenceImpl
import org.jetbrains.kotlin.library.abi.impl.TypeProjectionImpl

// This file contains Cursor methods specific to parsing klib dump files

internal fun Cursor.parseAbiModality(): AbiModality? =
    parseAbiModalityString()?.let { AbiModality.valueOf(it) }

internal fun Cursor.parseClassKind(): AbiClassKind? =
    parseClassKindString()?.let { AbiClassKind.valueOf(it) }

internal fun Cursor.parsePropertyKind(): AbiPropertyKind? =
    parsePropertyKindString()?.let { AbiPropertyKind.valueOf(it) }

internal fun Cursor.hasClassKind(): Boolean {
    val subCursor = copy()
    subCursor.skipInlineWhitespace()
    subCursor.parseAbiModality()
    subCursor.parseClassModifiers()
    return subCursor.parseClassKind() != null
}

internal fun Cursor.hasFunctionKind(): Boolean {
    val subCursor = copy()
    subCursor.skipInlineWhitespace()
    subCursor.parseAbiModality()
    subCursor.parseFunctionModifiers()
    return subCursor.parseFunctionKind() != null
}

internal fun Cursor.hasPropertyKind(): Boolean {
    val subCursor = copy()
    subCursor.skipInlineWhitespace()
    subCursor.parseAbiModality()
    return subCursor.parsePropertyKind() != null
}

internal fun Cursor.hasEnumEntry(): Boolean = parseEnumEntryKind(peek = true) != null

internal fun Cursor.hasGetter() = hasPropertyAccessor(GetterOrSetter.GETTER)

internal fun Cursor.hasSetter() = hasPropertyAccessor(GetterOrSetter.SETTER)

internal fun Cursor.hasGetterOrSetter() = hasGetter() || hasSetter()

internal fun Cursor.parseGetterName(peek: Boolean = false): String? {
    val cursor = subCursor(peek)
    cursor.parseSymbol(getterNamePrefix) ?: return null
    val name = cursor.parseValidIdentifier() ?: return null
    cursor.parseSymbol(closeAngleBracketSymbol) ?: return null
    return "<get-$name>"
}

internal fun Cursor.parseSetterName(peek: Boolean = false): String? {
    val cursor = subCursor(peek)
    cursor.parseSymbol(setterNamePrefix) ?: return null
    val name = cursor.parseValidIdentifier() ?: return null
    cursor.parseSymbol(closeAngleBracketSymbol) ?: return null
    return "<set-$name>"
}

internal fun Cursor.parseGetterOrSetterName(peek: Boolean = false) =
    parseGetterName(peek) ?: parseSetterName(peek)

internal fun Cursor.parseClassModifier(peek: Boolean = false): String? =
    parseSymbol(classModifierSymbols, peek)

internal fun Cursor.parseClassModifiers(): Set<String> {
    val modifiers = mutableSetOf<String>()
    while (parseClassModifier(peek = true) != null) {
        modifiers.add(parseClassModifier()!!)
    }
    return modifiers
}

internal fun Cursor.parseFunctionKind(peek: Boolean = false) = parseSymbol(functionKindSymbols, peek)

internal fun Cursor.parseFunctionModifier(peek: Boolean = false): String? =
    parseSymbol(functionModifierSymbols, peek)

internal fun Cursor.parseFunctionModifiers(): Set<String> {
    val modifiers = mutableSetOf<String>()
    while (parseFunctionModifier(peek = true) != null) {
        modifiers.add(parseFunctionModifier()!!)
    }
    return modifiers
}

internal fun Cursor.parseConstructorName() = parseSymbol(constructorNameSymbol)

// Valid identifiers can appear in a lot of places, some of them are followed by spaces,
// for example at the end of a class name ('class libname.Foo {'). But not at the end of a function
// name ('libname.foo()'). So we trim the whitespace only when we know it was inserted by the dump
// format and is not part of the identifier itself.
internal fun Cursor.parseValidIdentifierAndMaybeTrim(peek: Boolean = false, allowDot: Boolean = false) =
    when {
        allowDot -> parseSymbol(validIdentifierWithDotRegex, peek)
        else -> parseValidIdentifier(peek)
    }?.let {
        if (parseSymbol(symbolsFollowingIdentifiersWithSpaces, peek = true) != null) {
            it.dropLast(1)
        } else {
            it
        }
    }

internal fun Cursor.parseAbiQualifiedName(peek: Boolean = false): AbiQualifiedName? {
    val cursor = subCursor(peek)
    val packageName = cursor.parsePackageName() ?: ""
    cursor.parseSymbol(slashSymbol) ?: return null
    val relativeName = cursor.parseValidIdentifierAndMaybeTrim(allowDot = true)?.ifEmpty { return null } ?: return null
    return AbiQualifiedName(AbiCompoundName(packageName), AbiCompoundName(relativeName))
}

internal fun Cursor.parsePackageName(peek: Boolean = false) = parseSymbol(anythingButSlashRegex, peek)

internal fun Cursor.parseAbiType(peek: Boolean = false): AbiType? {
    val cursor = subCursor(peek)
    // A type will either be a qualified name (kotlin/Array) or a type reference (#A)
    // try to parse a qualified name and a type reference if it doesn't exist
    cursor.parseTypeReference()?.let {
        return it
    }
    val abiQualifiedName = cursor.parseAbiQualifiedName() ?: return null
    val typeArgs = cursor.parseTypeArgs() ?: emptyList()
    val nullability = cursor.parseNullability(assumeNotNull = true)
    return SimpleTypeImpl(
        ClassReferenceImpl(abiQualifiedName),
        arguments = typeArgs,
        nullability = nullability,
    )
}

internal fun Cursor.parseTypeArgs(): List<AbiTypeArgument>? {
    val typeArgsString = parseTypeParamsString() ?: return null
    val subCursor = Cursor(typeArgsString)
    subCursor.parseSymbol(openAngleBracketSymbol) ?: return null
    val typeArgs = mutableListOf<AbiTypeArgument>()
    while (subCursor.parseTypeArg(peek = true) != null) {
        typeArgs.add(subCursor.parseTypeArg()!!)
        subCursor.parseSymbol(commaSymbol)
    }
    return typeArgs
}

internal fun Cursor.parseTypeArg(peek: Boolean = false): AbiTypeArgument? {
    val cursor = subCursor(peek)
    val variance = cursor.parseAbiVariance()
    cursor.parseSymbol(starProjectionSymbol)?.let {
        return StarProjectionImpl
    }
    val type = cursor.parseAbiType(peek) ?: return null
    return TypeProjectionImpl(type = type, variance = variance)
}

internal fun Cursor.parseAbiVariance(): AbiVariance {
    val variance = parseSymbol(abiVarianceSymbols) ?: return AbiVariance.INVARIANT
    return AbiVariance.valueOf(variance.uppercase())
}

internal fun Cursor.parseTypeReference(): AbiType? {
    val typeParamReference = parseTag() ?: return null
    val typeArgs = parseTypeArgs() ?: emptyList()
    val nullability = parseNullability()
    return SimpleTypeImpl(
        TypeParameterReferenceImpl(typeParamReference),
        arguments = typeArgs,
        nullability = nullability,
    )
}

internal fun Cursor.parseTag() = parseSymbol(tagRegex)?.removePrefix("#")

internal fun Cursor.parseNullability(assumeNotNull: Boolean = false): AbiTypeNullability {
    val nullable = parseSymbol(nullableSymbolSymbol) != null
    val definitelyNotNull = parseSymbol(notNullSymbolSymbol) != null
    return when {
        nullable -> AbiTypeNullability.MARKED_NULLABLE
        definitelyNotNull -> AbiTypeNullability.DEFINITELY_NOT_NULL
        else ->
            if (assumeNotNull) {
                AbiTypeNullability.DEFINITELY_NOT_NULL
            } else {
                AbiTypeNullability.NOT_SPECIFIED
            }
    }
}

internal fun Cursor.parseSuperTypes(): MutableSet<AbiType> {
    parseSymbol(colonSymbol)
    val superTypes = mutableSetOf<AbiType>()
    while (parseAbiQualifiedName(peek = true) != null) {
        superTypes.add(parseAbiType()!!)
        parseSymbol(commaSymbol)
    }
    return superTypes
}

internal fun Cursor.parseTypeParams(peek: Boolean = false): List<AbiTypeParameter>? {
    val typeParamsString = parseTypeParamsString(peek) ?: return null
    val subCursor = Cursor(typeParamsString)
    subCursor.parseSymbol(openAngleBracketSymbol)
    val typeParams = mutableListOf<AbiTypeParameter>()
    while (subCursor.parseTypeParam(peek = true) != null) {
        typeParams.add(subCursor.parseTypeParam()!!)
        subCursor.parseSymbol(commaSymbol)
    }
    return typeParams
}

internal fun Cursor.parseTypeParam(peek: Boolean = false): AbiTypeParameter? {
    val cursor = subCursor(peek)
    val tag = cursor.parseTag() ?: return null
    cursor.parseSymbol(colonSymbol)
    val variance = cursor.parseAbiVariance()
    val isReified = cursor.parseSymbol(reifiedSymbol) != null
    val upperBounds = mutableListOf<AbiType>()
    while (null != cursor.parseAbiType(peek = true)) {
        upperBounds.add(cursor.parseAbiType()!!)
        cursor.parseSymbol(ampersandSymbol) ?: break
    }

    return AbiTypeParameterImpl(
        tag = tag,
        variance = variance,
        isReified = isReified,
        upperBounds = upperBounds,
    )
}

internal fun Cursor.parseValueParameters(
    kind: AbiValueParameterKind = AbiValueParameterKind.REGULAR
): List<AbiValueParameter>? {
    val valueParams = mutableListOf<AbiValueParameter>()
    parseSymbol(openParenSymbol) ?: return null
    if (parseSymbol(closeParenSymbol) != null) {
        return valueParams
    }
    while (null != parseValueParameter(kind, peek = true)) {
        valueParams.add(parseValueParameter(kind)!!)
        parseSymbol(commaSymbol) ?: break
    }
    parseSymbol(closeParenSymbol)
    return valueParams
}

internal fun Cursor.parseValueParameter(
    kind: AbiValueParameterKind = AbiValueParameterKind.REGULAR,
    peek: Boolean = false,
): AbiValueParameter? {
    val cursor = subCursor(peek)
    val modifiers = cursor.parseValueParameterModifiers()
    val isNoInline = modifiers.contains("noinline")
    val isCrossinline = modifiers.contains("crossinline")
    val type = cursor.parseAbiType() ?: return null
    val isVararg = cursor.parseVarargSymbol() != null
    val hasDefaultArg = cursor.parseDefaultArg() != null
    return AbiValueParameterImpl(
        kind = kind,
        type = type,
        isVararg = isVararg,
        hasDefaultArg = hasDefaultArg,
        isNoinline = isNoInline,
        isCrossinline = isCrossinline,
    )
}

internal fun Cursor.parseValueParameterModifiers(): Set<String> {
    val modifiers = mutableSetOf<String>()
    while (parseValueParameterModifier(peek = true) != null) {
        modifiers.add(parseValueParameterModifier()!!)
    }
    return modifiers
}

internal fun Cursor.parseValueParameterModifier(peek: Boolean = false): String? =
    parseSymbol(valueParameterModifierSymbols, peek)

internal fun Cursor.parseVarargSymbol() = parseSymbol(varargSymbol)

internal fun Cursor.parseDefaultArg() = parseSymbol(defaultArgSymbolRegex)

internal fun Cursor.parseContextAndReceiverParams(): List<AbiValueParameter> {
    val contextAndReceiverParams = mutableListOf<AbiValueParameter>()
    var inOpenParen = parseSymbol(openParenSymbol) != null
    parseContextParams()?.let { contextAndReceiverParams.addAll(it) }
    val afterClosedParen = parseSymbol(closeParenSymbol) != null
    // we get a second opening paren in old context formatting with a receiver
    // context(...) (receiver).
    inOpenParen = inOpenParen || parseSymbol(openParenSymbol) != null
    if (inOpenParen && !afterClosedParen) {
        parseFunctionReceiver()?.let { contextAndReceiverParams.add(it) }
    }
    parseSymbol(closeParenSymbol)
    parseSymbol(dotSymbol)
    return contextAndReceiverParams
}

private fun Cursor.parseFunctionReceiver(): AbiValueParameter? {
    val type = parseAbiType() ?: return null
    return AbiValueParameterImpl(
        kind = AbiValueParameterKind.EXTENSION_RECEIVER,
        type = type,
        isVararg = false,
        hasDefaultArg = false,
        isNoinline = false,
        isCrossinline = false,
    )
}

private fun Cursor.parseContextParams(): List<AbiValueParameter>? {
    parseSymbol(contextSymbol) ?: return null
    val params = parseValueParameters(AbiValueParameterKind.CONTEXT)

    return params
}

internal fun Cursor.parseReturnType(): AbiType? {
    parseSymbol(colonSymbol)
    return parseAbiType()
}

internal fun Cursor.hasLibraryUniqueName(): Boolean =
    parseSymbol(uniqueNameSymbol, peek = true) != null

internal fun Cursor.parseLibraryUniqueName(): String? {
    parseSymbol(uniqueNameSymbol)
    parseSymbol(openAngleBracketSymbol)
    return parseSymbol(uniqueNameRegex)
}

internal fun Cursor.hasSignatureVersion(): Boolean =
    parseSymbol(signatureSymbol, peek = true) != null

internal fun Cursor.parseSignatureVersion(): AbiSignatureVersion? {
    parseSymbol(signatureSymbol)
    val versionString = parseSymbol(digitRegex) ?: return null
    val versionNumber = versionString.toInt()
    return AbiSignatureVersion.resolveByVersionNumber(versionNumber)
}

internal fun Cursor.parseEnumEntryKind(peek: Boolean = false) =
    parseSymbol(enumEntryKindSymbol, peek)

internal fun Cursor.parseEnumEntryName() = parseSymbol(anythingButSlashRegex)

internal fun Cursor.parseCommentMarker() = parseSymbol(commentSymbol)

internal fun Cursor.parseOpenClassBody() = parseSymbol(openCurlyBraceSymbol)

internal fun Cursor.parseCloseClassBody(peek: Boolean = false) =
    parseSymbol(closeCurlyBraceSymbol, peek)

/**
 * Used to check if declarations after a property are getter / setter methods which should be
 * attached to that property.
 */
private fun Cursor.hasPropertyAccessor(type: GetterOrSetter): Boolean {
    val subCursor = copy()
    subCursor.parseAbiModality()
    subCursor.parseFunctionModifiers()
    subCursor.parseFunctionKind() ?: return false // if it's not a function it's not a getter/setter
    val mightHaveTypeParams = subCursor.parseGetterOrSetterName(peek = true) == null
    if (mightHaveTypeParams) {
        subCursor.parseTypeParams()
    }
    subCursor.parseContextAndReceiverParams()
    return when (type) {
        GetterOrSetter.GETTER -> subCursor.parseGetterName() != null
        GetterOrSetter.SETTER -> subCursor.parseSetterName() != null
    }
}

private fun Cursor.subCursor(peek: Boolean) =
    if (peek) {
        copy()
    } else {
        this
    }

private fun Cursor.parseTypeParamsString(peek: Boolean = false): String? {
    if (parseSymbol(getterOrSetterSymbols, peek = true) != null) {
        return null
    }
    val cursor = subCursor(peek)
    val result = StringBuilder()
    cursor.parseSymbol(openAngleBracketSymbol)?.let { result.append(it) } ?: return null
    var openBracketCount = 1
    while (openBracketCount > 0) {
        val nextSymbol =
            cursor.parseSymbol(anyCharRegex, skipInlineWhitespace = false).also {
                result.append(it)
            }
        when (nextSymbol) {
            "<" -> openBracketCount++
            ">" -> openBracketCount--
        }
    }
    cursor.skipInlineWhitespace()
    return result.toString()
}

private fun Cursor.parseAbiModalityString(peek: Boolean = false) =
    parseSymbol(abiModalitySymbols, peek)?.uppercase()

private fun Cursor.parsePropertyKindString(peek: Boolean = false) =
    parseSymbol(propertyKindSymbols, peek)?.uppercase()?.replace(" ", "_")

private fun Cursor.parseClassKindString(peek: Boolean = false) =
    parseSymbol(classKindSymbols, peek)?.uppercase()?.replace(" ", "_")

private enum class GetterOrSetter() {
    GETTER,
    SETTER,
}

private const val signatureSymbol = "- Signature version:"
private const val uniqueNameSymbol = "Library unique name: "
private const val getterNamePrefix = "<get-"
private const val setterNamePrefix = "<set-"
private const val reifiedSymbol = "reified"
private const val contextSymbol = "context"
private const val varargSymbol = "..."
private const val constructorNameSymbol = "constructor <init>"
private const val colonSymbol = ":"
private const val commaSymbol = ","
private const val commentSymbol = "//"
private const val enumEntryKindSymbol = "enum entry"
private const val openCurlyBraceSymbol = "{"
private const val starProjectionSymbol = "*"
private const val dotSymbol = "."
private const val slashSymbol = "/"
private const val ampersandSymbol = "&"
private const val openAngleBracketSymbol = "<"
private const val closeAngleBracketSymbol = ">"
private const val closeCurlyBraceSymbol = "}"
private const val nullableSymbolSymbol = "?"
private const val notNullSymbolSymbol = "!!"
private const val openParenSymbol = "("
private const val closeParenSymbol = ")"
private val getterOrSetterSymbols = listOf(getterNamePrefix, setterNamePrefix)
private val propertyKindSymbols = listOf("const val", "val", "var")
private val classModifierSymbols = listOf("inner", "value", "fun", "open")
private val functionKindSymbols = listOf("constructor", "fun")
private val functionModifierSymbols = listOf("inline", "suspend")
private val abiVarianceSymbols = listOf("out", "in")
private val valueParameterModifierSymbols = listOf("crossinline", "noinline")
private val abiModalitySymbols = listOf("final", "open", "abstract", "sealed")
private val classKindSymbols = listOf("class", "interface", "object", "enum class", "annotation class")
private val symbolsFollowingIdentifiersWithSpaces = listOf(":", "|", "/", "=", "{", "&")
private val anythingButSlashRegex = Regex("^[^/]*")
private val uniqueNameRegex = Regex("[a-zA-Z\\-.:]+")
private val anyCharRegex = Regex(".")
private val defaultArgSymbolRegex = Regex("^=(\\s)?\\.\\.\\.")
private val tagRegex = Regex("^#[a-zA-Z0-9]+")
private val digitRegex = Regex("^\\d+")

