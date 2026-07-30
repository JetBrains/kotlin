/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.decompiler.stub

import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import org.jetbrains.kotlin.constant.*
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.ClassIdBasedLocality
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.StandardClassIds
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.stubs.ConstantValueKind
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes
import org.jetbrains.kotlin.psi.stubs.impl.*

/**
 * Whether stubs can be built for the arguments by [createValueArgumentListStub].
 *
 * Only evaluated values are available in the metadata, and not every one of them has a counterpart in Kotlin sources.
 * Annotations with such arguments keep them in [KotlinAnnotationEntryStubImpl.valueArguments] only.
 */
internal fun Map<Name, ConstantValue<*>>.areRepresentableAsStubs(): Boolean = all { [name, value] ->
    // A special name cannot be rendered as an argument name
    !name.isSpecial && value.isRepresentableAsStub()
}

private fun ConstantValue<*>.isRepresentableAsStub(): Boolean = when (this) {
    is ArrayValue -> value.all { it.isRepresentableAsStub() }
    is AnnotationValue -> value.classId.isRepresentableAsStub && value.argumentsMapping.areRepresentableAsStubs()
    is EnumValue -> enumClassId.isRepresentableAsStub
    is KClassValue -> (value as? KClassValue.Value.NormalClass)?.classId?.isRepresentableAsStub == true
    is ErrorValue -> false
    else -> true
}

/**
 * A local class has no fully qualified name to refer to.
 *
 * Note: [createStubForTypeName] substitutes such classes with `kotlin.Any`, which is acceptable for types,
 * but would be misleading for a value.
 */
@OptIn(ClassIdBasedLocality::class)
private val ClassId.isRepresentableAsStub: Boolean
    get() = !isLocal

/**
 * Creates a [KtValueArgumentList] stub with a [KtValueArgument] per entry of [args].
 *
 * All arguments are named, as the metadata provides argument names but not their positions.
 * [areRepresentableAsStubs] has to be checked beforehand.
 */
internal fun createValueArgumentListStub(parent: StubElement<*>, args: Map<Name, ConstantValue<*>>) {
    val argumentList = KotlinPlaceHolderStubImpl<KtValueArgumentList>(parent, KtStubElementTypes.VALUE_ARGUMENT_LIST)
    for ([name, value] in args) {
        val argument = KotlinValueArgumentStubImpl(argumentList, KtStubElementTypes.VALUE_ARGUMENT, isSpread = false)
        val argumentName = KotlinPlaceHolderStubImpl<KtValueArgumentName>(argument, KtStubElementTypes.VALUE_ARGUMENT_NAME)
        createNameReferenceStub(argumentName, name)
        createValueStub(argument, value)
    }
}

private fun createValueStub(parent: StubElement<*>, value: ConstantValue<*>) {
    when (value) {
        is BooleanValue -> createConstantStub(parent, ConstantValueKind.BOOLEAN_CONSTANT, value.value.toString())
        is ByteValue -> createNumberStub(parent, ConstantValueKind.INTEGER_CONSTANT, value.value.toString())
        is ShortValue -> createNumberStub(parent, ConstantValueKind.INTEGER_CONSTANT, value.value.toString())
        is IntValue -> createNumberStub(parent, ConstantValueKind.INTEGER_CONSTANT, value.value.toString())
        is LongValue -> createNumberStub(parent, ConstantValueKind.INTEGER_CONSTANT, value.value.toString() + "L")
        is UByteValue -> createConstantStub(parent, ConstantValueKind.INTEGER_CONSTANT, value.stringTemplateValue() + "u")
        is UShortValue -> createConstantStub(parent, ConstantValueKind.INTEGER_CONSTANT, value.stringTemplateValue() + "u")
        is UIntValue -> createConstantStub(parent, ConstantValueKind.INTEGER_CONSTANT, value.stringTemplateValue() + "u")
        is ULongValue -> createConstantStub(parent, ConstantValueKind.INTEGER_CONSTANT, value.stringTemplateValue() + "uL")
        is DoubleValue -> createFloatingPointStub(parent, StandardClassIds.Double, value.value, value.value.toString())
        is FloatValue -> createFloatingPointStub(parent, StandardClassIds.Float, value.value.toDouble(), value.value.toString() + "F")
        is CharValue -> createConstantStub(parent, ConstantValueKind.CHARACTER_CONSTANT, renderCharacterLiteral(value.value))
        is NullValue -> createConstantStub(parent, ConstantValueKind.NULL, "null")
        is StringValue -> createStringTemplateStub(parent, value.value)
        is EnumValue -> createReferenceChainStub(parent, value.enumClassId.segments() + value.enumEntryName)
        is KClassValue -> createClassLiteralStub(parent, value.value as KClassValue.Value.NormalClass)
        is AnnotationValue -> createNestedAnnotationStub(parent, value.value)
        is ArrayValue -> createCollectionLiteralStub(parent, value.value)
        else -> error("Unexpected value: ${value::class}")
    }
}

/**
 * There are no negative literals in Kotlin, so a [KtPrefixExpression] is required to render one.
 *
 * Note: the absolute value of a minimal value doesn't fit into its type, but this is a compilation
 * error and not a parsing one, so the same tree is built for the reparsed text.
 */
@OptIn(KtImplementationDetail::class)
private fun createNumberStub(parent: StubElement<*>, kind: ConstantValueKind, text: String) {
    val positiveText = text.removePrefix("-")
    if (positiveText.length == text.length) {
        createConstantStub(parent, kind, text)
        return
    }

    val prefixExpression = KotlinPlaceHolderStubImpl<KtPrefixExpression>(parent, KtStubElementTypes.PREFIX_EXPRESSION)
    KotlinOperationReferenceExpressionStubImpl(prefixExpression, KtTokens.MINUS.value.ref(), KtTokens.MINUS)
    createConstantStub(prefixExpression, kind, positiveText)
}

/**
 * [Double.NaN] and the infinities have no literal form, so they are rendered as references to the corresponding constants.
 */
private fun createFloatingPointStub(parent: StubElement<*>, classId: ClassId, value: Double, text: String) {
    val constantName = when {
        value.isNaN() -> NAN_NAME
        value == Double.POSITIVE_INFINITY -> POSITIVE_INFINITY_NAME
        value == Double.NEGATIVE_INFINITY -> NEGATIVE_INFINITY_NAME
        else -> return createNumberStub(parent, ConstantValueKind.FLOAT_CONSTANT, text)
    }

    createReferenceChainStub(parent, classId.segments() + constantName)
}

private fun createStringTemplateStub(parent: StubElement<*>, value: String) {
    val template = KotlinPlaceHolderStubImpl<KtStringTemplateExpression>(parent, KtStubElementTypes.STRING_TEMPLATE)
    val literal = StringBuilder()

    fun flushLiteral() {
        if (literal.isEmpty()) return
        KotlinPlaceHolderWithTextStubImpl<KtLiteralStringTemplateEntry>(
            template,
            KtStubElementTypes.LITERAL_STRING_TEMPLATE_ENTRY,
            literal.toString(),
        )

        literal.clear()
    }

    for (character in value) {
        val escaped = character.escapedIn(quote = '"')
        if (escaped == null) {
            literal.append(character)
        } else {
            flushLiteral()
            KotlinPlaceHolderWithTextStubImpl<KtEscapeStringTemplateEntry>(
                template,
                KtStubElementTypes.ESCAPE_STRING_TEMPLATE_ENTRY,
                escaped,
            )
        }
    }

    flushLiteral()
}

private fun createClassLiteralStub(parent: StubElement<*>, value: KClassValue.Value.NormalClass) {
    val classLiteral = KotlinClassLiteralExpressionStubImpl(parent)
    val arrayDimensions = value.arrayDimensions
    if (arrayDimensions == 0) {
        createReferenceChainStub(classLiteral, value.classId.segments())
        return
    }

    // An array class literal has no separate class id, so it is rendered as `kotlin.Array<...>::class`
    createQualifiedStub(classLiteral, StandardClassIds.Array.segments().dropLast(1)) { receiver ->
        val call = KotlinPlaceHolderStubImpl<KtCallExpression>(receiver, KtStubElementTypes.CALL_EXPRESSION)
        createNameReferenceStub(call, StandardClassIds.Array.shortClassName)
        createTypeArgumentListStub(call) { typeReference ->
            createArrayTypeStub(typeReference, value.classId, arrayDimensions - 1)
        }
    }
}

private fun createArrayTypeStub(parent: StubElement<*>, classId: ClassId, arrayDimensions: Int) {
    if (arrayDimensions == 0) {
        createStubForTypeName(classId, parent)
        return
    }

    createStubForTypeName(StandardClassIds.Array, parent) { userType, level ->
        // Type arguments always belong to the innermost user type
        if (level == 0) {
            createTypeArgumentListStub(userType) { typeReference ->
                createArrayTypeStub(typeReference, classId, arrayDimensions - 1)
            }
        }
    }
}

private fun createTypeArgumentListStub(parent: StubElement<*>, createType: (StubElement<*>) -> Unit) {
    val typeArgumentList = KotlinPlaceHolderStubImpl<KtTypeArgumentList>(parent, KtStubElementTypes.TYPE_ARGUMENT_LIST)
    val typeProjection = KotlinTypeProjectionStubImpl(typeArgumentList, KtProjectionKind.NONE.ordinal)
    val typeReference = KotlinPlaceHolderStubImpl<KtTypeReference>(typeProjection, KtStubElementTypes.TYPE_REFERENCE)
    createType(typeReference)
}

private fun createNestedAnnotationStub(parent: StubElement<*>, value: AnnotationValue.Value) {
    val segments = value.classId.segments()
    createQualifiedStub(parent, segments.dropLast(1)) { receiver ->
        val call = KotlinPlaceHolderStubImpl<KtCallExpression>(receiver, KtStubElementTypes.CALL_EXPRESSION)
        createNameReferenceStub(call, segments.last())

        // The list is created even for an empty mapping, otherwise the text would be rendered as a plain reference
        createValueArgumentListStub(call, value.argumentsMapping)
    }
}

private fun createCollectionLiteralStub(parent: StubElement<*>, values: List<ConstantValue<*>>) {
    val collectionLiteral = KotlinCollectionLiteralExpressionStubImpl(parent, innerExpressionCount = values.size)
    for (value in values) {
        createValueStub(collectionLiteral, value)
    }
}

private fun createConstantStub(parent: StubElement<*>, kind: ConstantValueKind, text: String) {
    KotlinConstantExpressionStubImpl(parent, kind, text.ref())
}

private fun createNameReferenceStub(parent: StubElement<*>, name: Name) {
    KotlinNameReferenceExpressionStubImpl(parent, name.ref(), /* myClassRef = */ false)
}

/**
 * Creates a [KtDotQualifiedExpression] chain of plain references, e.g., `kotlin.DeprecationLevel.HIDDEN`.
 */
private fun createReferenceChainStub(parent: StubElement<*>, segments: List<Name>) {
    createQualifiedStub(parent, segments.dropLast(1)) { receiver ->
        createNameReferenceStub(receiver, segments.last())
    }
}

/**
 * Creates a [KtDotQualifiedExpression] with a reference chain of [receiverSegments] as its receiver
 * and the result of [createSelector] as its selector.
 *
 * The selector is created in [parent] directly if there is no receiver to qualify it with.
 */
private fun createQualifiedStub(
    parent: StubElement<*>,
    receiverSegments: List<Name>,
    createSelector: (StubElement<*>) -> Unit,
) {
    if (receiverSegments.isEmpty()) {
        createSelector(parent)
        return
    }

    val qualifiedExpression = KotlinPlaceHolderStubImpl<KtDotQualifiedExpression>(
        parent,
        KtStubElementTypes.DOT_QUALIFIED_EXPRESSION,
    )

    createReferenceChainStub(qualifiedExpression, receiverSegments)
    createSelector(qualifiedExpression)
}

private fun renderCharacterLiteral(value: Char): String = "'" + (value.escapedIn(quote = '\'') ?: value) + "'"

/**
 * The escape sequence for [this] character inside a literal delimited by [quote], or `null` if the character
 * can be rendered as is.
 *
 * Non-printable characters have to be escaped as the decompiled text is expected to be readable,
 * and some of them (e.g., a line separator) would break the literal.
 */
private fun Char.escapedIn(quote: Char): String? = when (this) {
    '\\' -> ESCAPE + '\\'
    quote -> ESCAPE + quote
    '$' -> ESCAPE + '$'
    '\n' -> ESCAPE + 'n'
    '\r' -> ESCAPE + 'r'
    '\t' -> ESCAPE + 't'
    '\b' -> ESCAPE + 'b'
    else -> if (isPrintable()) null else ESCAPE + UNICODE_ESCAPE_MARKER + "%04X".format(code)
}

/**
 * Mirrors the printability check of [org.jetbrains.kotlin.constant.CharValue].
 */
private fun Char.isPrintable(): Boolean = when (Character.getType(this).toByte()) {
    Character.UNASSIGNED,
    Character.LINE_SEPARATOR,
    Character.PARAGRAPH_SEPARATOR,
    Character.CONTROL,
    Character.FORMAT,
    Character.PRIVATE_USE,
    Character.SURROGATE,
        -> false

    else -> true
}

private fun ClassId.segments(): List<Name> = asSingleFqName().pathSegments()

private fun String.ref(): StringRef = StringRef.fromString(this)!!

private val NAN_NAME = Name.identifier("NaN")
private val POSITIVE_INFINITY_NAME = Name.identifier("POSITIVE_INFINITY")
private val NEGATIVE_INFINITY_NAME = Name.identifier("NEGATIVE_INFINITY")

private const val ESCAPE = "\\"
private const val UNICODE_ESCAPE_MARKER = 'u'
