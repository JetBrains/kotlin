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

internal fun Cursor.parseAbiModality(): AbiModality? {
    val parsed = parseAbiModalityString(peek = true)?.let { AbiModality.valueOf(it) }
    if (parsed != null) {
        parseAbiModalityString()
    }
    return parsed
}

internal fun Cursor.parseClassKind(peek: Boolean = false): AbiClassKind? {
    val parsed = parseClassKindString(peek = true)?.let { AbiClassKind.valueOf(it) }
    if (parsed != null && !peek) {
        parseClassKindString()
    }
    return parsed
}

internal fun Cursor.parsePropertyKind(peek: Boolean = false): AbiPropertyKind? {
    val parsed = parsePropertyKindString(peek = true)?.let { AbiPropertyKind.valueOf(it) }
    if (parsed != null && !peek) {
        parsePropertyKindString()
    }
    return parsed
}

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
    cursor.parseContextParams()
    cursor.parseFunctionReceiver()
    cursor.parseSymbol(getterNameRegex) ?: return null
    val name = cursor.parseValidIdentifier() ?: return null
    cursor.parseSymbol(closeAngleBracketRegex) ?: return null
    return "<get-$name>"
}

internal fun Cursor.parseSetterName(peek: Boolean = false): String? {
    val cursor = subCursor(peek)
    cursor.parseContextParams()
    cursor.parseFunctionReceiver()
    cursor.parseSymbol(setterNameRegex) ?: return null
    val name = cursor.parseValidIdentifier() ?: return null
    cursor.parseSymbol(closeAngleBracketRegex) ?: return null
    return "<set-$name>"
}

internal fun Cursor.parseGetterOrSetterName(peek: Boolean = false) =
    parseGetterName(peek) ?: parseSetterName(peek)

internal fun Cursor.parseClassModifier(peek: Boolean = false): String? =
    parseSymbol(classModifierRegex, peek)

internal fun Cursor.parseClassModifiers(): Set<String> {
    val modifiers = mutableSetOf<String>()
    while (parseClassModifier(peek = true) != null) {
        modifiers.add(parseClassModifier()!!)
    }
    return modifiers
}

internal fun Cursor.parseFunctionKind(peek: Boolean = false) = parseSymbol(functionKindRegex, peek)

internal fun Cursor.parseFunctionModifier(peek: Boolean = false): String? =
    parseSymbol(functionModifierRegex, peek)

internal fun Cursor.parseFunctionModifiers(): Set<String> {
    val modifiers = mutableSetOf<String>()
    while (parseFunctionModifier(peek = true) != null) {
        modifiers.add(parseFunctionModifier()!!)
    }
    return modifiers
}

internal fun Cursor.parseConstructorName() = parseSymbol(constructorNameRegex)

// Valid identifiers can appear in a lot of places, some of them are followed by spaces,
// for example at the end of a class name ('class libname.Foo {'). But not at the end of a function
// name ('libname.foo()'). So we trim the whitespace only when we know it was inserted by the dump
// format and is not part of the identifier itself.
internal fun Cursor.parseValidIdentifierAndMaybeTrim(peek: Boolean = false) =
    parseValidIdentifier(peek)?.let {
        if (parseSymbol(symbolsFollowingIdentifiersWithSpaces, peek = true) != null) {
            it.dropLast(1)
        } else {
            it
        }
    }

internal fun Cursor.parseAbiQualifiedName(peek: Boolean = false): AbiQualifiedName? {
    val cursor = subCursor(peek)
    val packageName = cursor.parsePackageName() ?: return null
    cursor.parseSymbol(slashRegex) ?: return null
    val relativeNameBuilder = StringBuilder()
    while (cursor.hasNextValidIdentifierPiece()) {
        cursor.parseSymbol(dotRegex)?.let { relativeNameBuilder.append(it) }
        relativeNameBuilder.append(cursor.parseValidIdentifierAndMaybeTrim())
    }
    val relativeName =
        relativeNameBuilder.toString().ifEmpty {
            return null
        }
    return AbiQualifiedName(AbiCompoundName(packageName), AbiCompoundName(relativeName))
}

private fun Cursor.hasNextValidIdentifierPiece(): Boolean {
    val cursor = subCursor(peek = true)
    cursor.parseSymbol(dotRegex)
    return cursor.parseValidIdentifier(peek = true) != null
}

internal fun Cursor.parsePackageName() = parseSymbol(packageNameRegex)

internal fun Cursor.parseAbiType(peek: Boolean = false): AbiType? {
    val cursor = subCursor(peek)
    // A type will either be a qualified name (kotlin/Array) or a type reference (#A)
    // try to parse a qualified name and a type reference if it doesn't exist
    val abiQualifiedName = cursor.parseAbiQualifiedName() ?: return cursor.parseTypeReference()
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
    subCursor.parseSymbol(openAngleBracketRegex) ?: return null
    val typeArgs = mutableListOf<AbiTypeArgument>()
    while (subCursor.parseTypeArg(peek = true) != null) {
        typeArgs.add(subCursor.parseTypeArg()!!)
        subCursor.parseSymbol(commaRegex)
    }
    return typeArgs
}

internal fun Cursor.parseTypeArg(peek: Boolean = false): AbiTypeArgument? {
    val cursor = subCursor(peek)
    val variance = cursor.parseAbiVariance()
    cursor.parseSymbol(starProjectionRegex)?.let {
        return StarProjectionImpl
    }
    val type = cursor.parseAbiType(peek) ?: return null
    return TypeProjectionImpl(type = type, variance = variance)
}

internal fun Cursor.parseAbiVariance(): AbiVariance {
    val variance = parseSymbol(abiVarianceRegex) ?: return AbiVariance.INVARIANT
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
    val nullable = parseSymbol(nullableSymbolRegex) != null
    val definitelyNotNull = parseSymbol(notNullSymbolRegex) != null
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
    parseSymbol(colonRegex)
    val superTypes = mutableSetOf<AbiType>()
    while (parseAbiQualifiedName(peek = true) != null) {
        superTypes.add(parseAbiType()!!)
        parseSymbol(commaRegex)
    }
    return superTypes
}

internal fun Cursor.parseTypeParams(peek: Boolean = false): List<AbiTypeParameter>? {
    val typeParamsString = parseTypeParamsString(peek) ?: return null
    val subCursor = Cursor(typeParamsString)
    subCursor.parseSymbol(openAngleBracketRegex)
    val typeParams = mutableListOf<AbiTypeParameter>()
    while (subCursor.parseTypeParam(peek = true) != null) {
        typeParams.add(subCursor.parseTypeParam()!!)
        subCursor.parseSymbol(commaRegex)
    }
    return typeParams
}

internal fun Cursor.parseTypeParam(peek: Boolean = false): AbiTypeParameter? {
    val cursor = subCursor(peek)
    val tag = cursor.parseTag() ?: return null
    cursor.parseSymbol(colonRegex)
    val variance = cursor.parseAbiVariance()
    val isReified = cursor.parseSymbol(reifiedRegex) != null
    val upperBounds = mutableListOf<AbiType>()
    while (null != cursor.parseAbiType(peek = true)) {
        upperBounds.add(cursor.parseAbiType()!!)
        cursor.parseSymbol(ampersandRegex)
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
    parseSymbol(openParenRegex)
    while (null != parseValueParameter(kind, peek = true)) {
        valueParams.add(parseValueParameter(kind)!!)
        parseSymbol(commaRegex)
    }
    parseSymbol(closeParenRegex)
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
    parseSymbol(valueParameterModifierRegex, peek)

internal fun Cursor.parseVarargSymbol() = parseSymbol(varargSymbolRegex)

internal fun Cursor.parseDefaultArg() = parseSymbol(defaultArgSymbolRegex)

internal fun Cursor.parseFunctionReceiver(): AbiType? {
    parseSymbol(openParenRegex) ?: return null
    val type = parseAbiType()
    parseSymbol(closeParenRegex)
    parseSymbol(dotRegex)
    return type
}

internal fun Cursor.parseContextParams(): List<AbiValueParameter>? {
    parseSymbol(contextRegex) ?: return null
    return parseValueParameters(AbiValueParameterKind.CONTEXT)
}

internal fun Cursor.parseReturnType(): AbiType? {
    parseSymbol(colonRegex)
    return parseAbiType()
}

internal fun Cursor.hasTargets(): Boolean = parseSymbol(targetsRegex, peek = true) != null

internal fun Cursor.parseTargets(): List<String> {
    parseSymbol(targetsRegex)
    parseSymbol(openSquareBracketRegex)
    val targets = mutableListOf<String>()
    while (parseValidIdentifier(peek = true) != null) {
        targets.add(parseValidIdentifier()!!)
        parseSymbol(commaRegex)
    }
    parseSymbol(closeSquareBracketRegex)
    return targets
}

internal fun Cursor.hasUniqueName(): Boolean =
    parseSymbol(uniqueNameMarkerRegex, peek = true) != null

internal fun Cursor.parseUniqueName(): String? {
    parseSymbol(uniqueNameMarkerRegex)
    parseSymbol(openAngleBracketRegex)
    return parseSymbol(uniqueNameRegex)
}

internal fun Cursor.hasSignatureVersion(): Boolean =
    parseSymbol(signatureMarkerRegex, peek = true) != null

internal fun Cursor.parseSignatureVersion(): AbiSignatureVersion? {
    parseSymbol(signatureMarkerRegex)
    val versionString = parseSymbol(digitRegex) ?: return null
    val versionNumber = versionString.toInt()
    return AbiSignatureVersion.resolveByVersionNumber(versionNumber)
}

internal fun Cursor.parseEnumEntryKind(peek: Boolean = false) =
    parseSymbol(enumEntryKindRegex, peek)

internal fun Cursor.parseEnumName() = parseSymbol(enumNameRegex)

internal fun Cursor.parseCommentMarker() = parseSymbol(commentMarkerRegex)

internal fun Cursor.parseOpenClassBody() = parseSymbol(openCurlyBraceRegex)

internal fun Cursor.parseCloseClassBody(peek: Boolean = false) =
    parseSymbol(closeCurlyBraceRegex, peek)

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
    subCursor.parseContextParams()
    subCursor.parseFunctionReceiver()
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
    if (parseSymbol(getterOrSetterSignalRegex, peek = true) != null) {
        return null
    }
    val cursor = subCursor(peek)
    val result = StringBuilder()
    cursor.parseSymbol(openAngleBracketRegex)?.let { result.append(it) } ?: return null
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
    parseSymbol(abiModalityRegex, peek)?.uppercase()

private fun Cursor.parsePropertyKindString(peek: Boolean = false) =
    parseSymbol(propertyKindRegex, peek)?.uppercase()?.replace(" ", "_")

private fun Cursor.parseClassKindString(peek: Boolean = false) =
    parseSymbol(classKindRegex, peek)?.uppercase()?.replace(" ", "_")

private enum class GetterOrSetter() {
    GETTER,
    SETTER,
}

private val constructorNameRegex = Regex("^constructor\\s<init>")
private val closeCurlyBraceRegex = Regex("^}")
private val uniqueNameMarkerRegex = Regex("^Library unique name: ")
private val uniqueNameRegex = Regex("[a-zA-Z\\-\\.:]+")
private val commentMarkerRegex = Regex("^\\/\\/")
private val anyCharRegex = Regex(".")
private val closeSquareBracketRegex = Regex("^\\]")
private val openSquareBracketRegex = Regex("^\\[")
private val targetsRegex = Regex("^Targets:")
private val defaultArgSymbolRegex = Regex("^=(\\s)?\\.\\.\\.")
private val varargSymbolRegex = Regex("^\\.\\.\\.")
private val openParenRegex = Regex("^\\(")
private val closeParenRegex = Regex("^\\)")
private val reifiedRegex = Regex("reified")
private val contextRegex = Regex("^context")
private val colonRegex = Regex("^:")
private val commaRegex = Regex("^,")
private val notNullSymbolRegex = Regex("^\\!\\!")
private val nullableSymbolRegex = Regex("^\\?")
private val tagRegex = Regex("^#[a-zA-Z0-9]+")
private val getterNameRegex = Regex("^<get\\-")
private val setterNameRegex = Regex("^<set\\-")
private val classModifierRegex = Regex("^(inner|value|fun|open)")
private val functionKindRegex = Regex("^(constructor|fun)")
private val functionModifierRegex = Regex("^(inline|suspend)")
private val packageNameRegex = Regex("^[a-zA-Z0-9.]+")
private val openAngleBracketRegex = Regex("^<")
private val closeAngleBracketRegex = Regex("^>")
private val openCurlyBraceRegex = Regex("^\\{")
private val starProjectionRegex = Regex("^\\*")
private val abiVarianceRegex = Regex("^(out|in)")
private val valueParameterModifierRegex = Regex("^(crossinline|noinline)")
private val abiModalityRegex = Regex("^(final|open|abstract|sealed)")
private val classKindRegex = Regex("^(class|interface|object|enum\\sclass|annotation\\sclass)")
private val propertyKindRegex = Regex("^(const\\sval|val|var)")
private val getterOrSetterSignalRegex = Regex("^<(get|set)\\-")
private val enumNameRegex = Regex("^[A-Z_]+")
private val enumEntryKindRegex = Regex("^enum\\sentry")
private val signatureMarkerRegex = Regex("-\\sSignature\\sversion:")
private val digitRegex = Regex("^\\d+")
private val dotRegex = Regex("^\\.")
private val slashRegex = Regex("^/")
private val symbolsFollowingIdentifiersWithSpaces = Regex("^[:|/={&]")
private val ampersandRegex = Regex("^&")
