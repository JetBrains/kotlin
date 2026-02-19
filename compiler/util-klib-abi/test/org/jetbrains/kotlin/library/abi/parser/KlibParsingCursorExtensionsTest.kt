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

@file:OptIn(ExperimentalLibraryAbiReader::class)

package org.jetbrains.kotlin.library.abi.parser


import org.jetbrains.kotlin.library.abi.AbiClassKind
import org.jetbrains.kotlin.library.abi.AbiModality
import org.jetbrains.kotlin.library.abi.AbiPropertyKind
import org.jetbrains.kotlin.library.abi.AbiTypeArgument
import org.jetbrains.kotlin.library.abi.AbiTypeNullability
import org.jetbrains.kotlin.library.abi.AbiValueParameter
import org.jetbrains.kotlin.library.abi.AbiValueParameterKind
import org.jetbrains.kotlin.library.abi.AbiVariance
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue

class KlibParsingCursorExtensionsTest {

    @Test
    fun parseModalityFailure() {
        val input = "something else"
        val cursor = Cursor(input)
        val modality = cursor.parseAbiModality()
        assertNull(modality)
        assertEquals("something else", cursor.currentLine)
    }

    @Test
    fun parseModalitySuccess() {
        val input = "final whatever"
        val cursor = Cursor(input)
        val modality = cursor.parseAbiModality()
        assertEquals(AbiModality.FINAL, modality)
        assertEquals("whatever", cursor.currentLine)
    }

    @Test
    fun parseClassModifier() {
        val input = "inner whatever"
        val cursor = Cursor(input)
        val modifier = cursor.parseClassModifier()
        assertEquals("inner", modifier)
        assertEquals("whatever", cursor.currentLine)
    }

    @Test
    fun parseClassModifiers() {
        val input = "inner value fun whatever"
        val cursor = Cursor(input)
        val modifiers = cursor.parseClassModifiers()
        assertIterableEquals(listOf("inner", "value", "fun"), modifiers)
        assertEquals("whatever", cursor.currentLine)
    }

    @Test
    fun parseFunctionModifiers() {
        val input = "final inline suspend fun myFun(): kotlin/Long"
        val cursor = Cursor(input)
        cursor.parseAbiModality()
        val modifiers = cursor.parseFunctionModifiers()
        assertIterableEquals(listOf("inline", "suspend"), modifiers)
        assertEquals("fun myFun(): kotlin/Long", cursor.currentLine)
    }

    @Test
    fun parseClassKindSimple() {
        val input = "class"
        val cursor = Cursor(input)
        val kind = cursor.parseClassKind()
        assertEquals(AbiClassKind.CLASS, kind)
    }

    @Test
    fun parseClassKindFalsePositive() {
        val input = "my.lib/notAClass"
        val cursor = Cursor(input)
        val kind = cursor.parseClassKind()
        assertNull(kind)
    }

    @Test
    fun parseClassKindMultiPart() {
        val input = "annotation class"
        val cursor = Cursor(input)
        val kind = cursor.parseClassKind()
        assertEquals(AbiClassKind.ANNOTATION_CLASS, kind)
    }

    @Test
    fun hasClassKind() {
        val input = "final class my.lib/MyClass"
        val cursor = Cursor(input)
        assertTrue(cursor.hasClassKind())
        assertEquals(input, cursor.currentLine)
    }

    @Test
    fun parseFunctionKindSimple() {
        val input = "fun hello"
        val cursor = Cursor(input)
        val kind = cursor.parseFunctionKind()
        assertEquals("fun", kind)
        assertEquals(cursor.currentLine, cursor.currentLine)
    }

    @Test
    fun hasFunctionKind() {
        val input = "    final fun myFun(): kotlin/String "
        val cursor = Cursor(input)
        assertTrue(cursor.hasFunctionKind())
        assertEquals(input, cursor.currentLine)
    }

    @Test
    fun hasFunctionKindConstructor() {
        val input = "    constructor <init>(kotlin/Int =...)"
        val cursor = Cursor(input)
        assertTrue(cursor.hasFunctionKind())
        assertEquals(input, cursor.currentLine)
    }

    @Test
    fun parseGetterOrSetterName() {
        val input = "<get-example>()"
        val cursor = Cursor(input)
        val name = cursor.parseGetterOrSetterName()
        assertEquals("<get-example>", name)
        assertEquals("()", cursor.currentLine)
    }

    @Test
    fun hasGetter() {
        val input = "final inline fun <get-example>(): kotlin.ranges/IntRange"
        val cursor = Cursor(input)
        assertTrue(cursor.hasGetter())
        assertEquals(input, cursor.currentLine)
    }

    @Test
    fun hasSetter() {
        val input = "final inline fun <set-example>(): kotlin.ranges/IntRange"
        val cursor = Cursor(input)
        assertTrue(cursor.hasSetter())
        assertEquals(input, cursor.currentLine)
    }

    @Test
    fun hasGetterOrSetter() {
        val inputs =
            listOf(
                "final inline fun <set-example>(): kotlin.ranges/IntRange",
                "final inline fun <get-example>(): kotlin.ranges/IntRange",
            )
        inputs.forEach { input -> assertTrue(Cursor(input).hasGetterOrSetter()) }
    }

    @Test
    fun hasPropertyKind() {
        val input = "final const val my.lib/myProp"
        val cursor = Cursor(input)
        assertTrue(cursor.hasPropertyKind())
        assertEquals(input, cursor.currentLine)
    }

    @Test
    fun parsePropertyKindConstVal() {
        val input = "const val something"
        val cursor = Cursor(input)
        val kind = cursor.parsePropertyKind()
        assertEquals(AbiPropertyKind.CONST_VAL, kind)
        assertEquals("something", cursor.currentLine)
    }

    @Test
    fun parsePropertyKindVal() {
        val input = "val something"
        val cursor = Cursor(input)
        val kind = cursor.parsePropertyKind()
        assertEquals(AbiPropertyKind.VAL, kind)
        assertEquals("something", cursor.currentLine)
    }

    @Test
    fun parseNullability() {
        val nullable = Cursor("?").parseNullability()
        val notNull = Cursor("!!").parseNullability()
        val unspecified = Cursor("another symbol").parseNullability()
        assertEquals(AbiTypeNullability.MARKED_NULLABLE, nullable)
        assertEquals(AbiTypeNullability.DEFINITELY_NOT_NULL, notNull)
        assertEquals(AbiTypeNullability.NOT_SPECIFIED, unspecified)
    }

    @Test
    fun parseNullabilityWhenAssumingNotNullable() {
        val unspecified = Cursor("").parseNullability(assumeNotNull = true)
        assertEquals(AbiTypeNullability.DEFINITELY_NOT_NULL, unspecified)
    }

    @Test
    fun parseQualifiedName() {
        val input = "my.lib/MyClass { something"
        val cursor = Cursor(input)
        val qName = cursor.parseAbiQualifiedName()
        assertEquals("my.lib/MyClass", qName.toString())
        assertEquals("{ something", cursor.currentLine)
    }

    @Test
    fun parseQualifiedNameKotlin() {
        val input = "kotlin/Function2<#A1, #A, #A1>"
        val cursor = Cursor(input)
        val qName = cursor.parseAbiQualifiedName()
        assertEquals("kotlin/Function2", qName.toString())
        assertEquals("<#A1, #A, #A1>", cursor.currentLine)
    }

    @Test
    fun parseQualifiedNameWithPackageNameWithSpaces() {
        val input = "name with space /myFun(): kotlin/Int"
        val cursor = Cursor(input)
        val qName = cursor.parseAbiQualifiedName()
        assertEquals("name with space /myFun", qName.toString())
    }

    @Test
    fun parseQualifie0dNameDoesNotGrabNullable() {
        val input = "my.lib/MyClass? something"
        val cursor = Cursor(input)
        val qName = cursor.parseAbiQualifiedName()
        assertEquals("my.lib/MyClass", qName.toString())
        assertEquals("? something", cursor.currentLine)
    }

    @Test
    fun parseQualifiedNameWithQualifiedReceiver() {
        val input = "my.longer.lib/MyClass.Builder.Inner {"
        val cursor = Cursor(input)
        val qName = cursor.parseAbiQualifiedName()
        assertEquals("my.longer.lib/MyClass.Builder.Inner", qName.toString())
    }

    @Test
    fun parseQualifiedNameBeforeDefaultParameterSymbol() {
        val input = "my.lib/MyClass =..."
        val cursor = Cursor(input)
        val qName = cursor.parseAbiQualifiedName()
        assertEquals("my.lib/MyClass", qName.toString())
    }

    @Test
    fun parseQualifiedNameWhenPackageIsBlank() {
        val input = "my.lib/MyClass =..."
        val cursor = Cursor(input)
        val qName = cursor.parseAbiQualifiedName()
        assertEquals("my.lib/MyClass", qName.toString())
    }

    @Test
    fun parseAbiType() {
        val input = "my.lib/MyClass<#A, #B> something"
        val cursor = Cursor(input)
        val type = cursor.parseAbiType()!!
        assertEquals("my.lib/MyClass", type.classNameOrTag)
        assertEquals(type.arguments!!.size, 2)
        assertEquals("something", cursor.currentLine)
    }

    @Test
    fun parseAbiTypeWithAnotherType() {
        val input =
            "my.lib/MyClass<#A, #B>, my.lib/Other<#A, #B> " +
                "something"
        val cursor = Cursor(input)
        val type = cursor.parseAbiType()
        assertEquals("my.lib/MyClass", type?.classNameOrTag)
        assertEquals(", my.lib/Other<#A, #B> something", cursor.currentLine)
    }

    @Test
    fun parseAbiTypeWithThreeParams() {
        val input = "kotlin/Function2<#A1, #A, #A1>"
        val cursor = Cursor(input)
        val type = cursor.parseAbiType()
        assertEquals("kotlin/Function2", type?.classNameOrTag)
    }

    @Test
    fun parseSuperTypes() {
        val input =
            ": my.lib/MyClass<#A, #B>, my.lib/Other<#A, #B> something"
        val cursor = Cursor(input)
        val superTypes = cursor.parseSuperTypes().toList()
        assertEquals(2, superTypes.size)
        assertEquals("my.lib/MyClass", superTypes.first().classNameOrTag)
        assertEquals("my.lib/Other", superTypes.last().classNameOrTag)
        assertEquals("something", cursor.currentLine)
    }

    @Test
    fun parseReturnType() {
        val input = ": my.lib/MyClass<#A, #B> stuff"
        val cursor = Cursor(input)
        val returnType = cursor.parseReturnType()
        assertEquals("my.lib/MyClass", returnType?.classNameOrTag)
        assertEquals("stuff", cursor.currentLine)
    }

    @Test
    fun parseReturnTypeNullableWithTypeParamsNullable() {
        val input = ": #B? stuff"
        val cursor = Cursor(input)
        val returnType = cursor.parseReturnType()
        assertEquals("B", returnType?.tag)
        assertEquals(AbiTypeNullability.MARKED_NULLABLE, returnType?.nullability)
        assertEquals("stuff", cursor.currentLine)
    }

    @Test
    fun parseReturnTypeNullableWithTypeParamsNotSpecified() {
        val input = ": #B stuff"
        val cursor = Cursor(input)
        val returnType = cursor.parseReturnType()
        assertEquals("B", returnType?.tag)
        assertEquals(AbiTypeNullability.NOT_SPECIFIED, returnType?.nullability)
        assertEquals("stuff", cursor.currentLine)
    }

    @Test
    fun parseFunctionReceiver() {
        val input = "(my.lib/LongSparseArray<#A>).my.lib/keyIterator()"
        val cursor = Cursor(input)
        val receiver = cursor.parseContextAndReceiverParams().single().type
        assertEquals("my.lib/LongSparseArray", receiver.classNameOrTag)
        assertEquals("my.lib/keyIterator()", cursor.currentLine)
    }

    @Test
    fun parseFunctionReceiver2() {
        val input = "(my.lib/LongSparseArray<#A1>).<get-size>(): kotlin/Int"
        val cursor = Cursor(input)
        val receiver = cursor.parseContextAndReceiverParams().single().type
        assertEquals("my.lib/LongSparseArray", receiver.classNameOrTag)
        assertEquals("<get-size>(): kotlin/Int", cursor.currentLine)
    }

    @Test
    fun parseFunctionReceiverWithStarParams() {
        val input = "(my.lib/MyClass<*, *>).<get-myExample>()"
        val cursor = Cursor(input)
        val receiver = cursor.parseContextAndReceiverParams().single().type
        assertEquals("my.lib/MyClass", receiver.classNameOrTag)
        assertEquals("<get-myExample>()", cursor.currentLine)
    }

    @Test
    fun parseFunctionReceiverWithNullableParam() {
        val input = "(kotlin.collections/List<#A?>)."
        val cursor = Cursor(input)
        val receiver = cursor.parseContextAndReceiverParams().single().type
        assertEquals("kotlin.collections/List", receiver.classNameOrTag)
        val typeArg = receiver.arguments?.single()?.type
        assertEquals(AbiTypeNullability.MARKED_NULLABLE, typeArg?.nullability)
        assertEquals("A", typeArg?.tag)
    }

    @Test
    fun parseSimpleContextParams() {
        val input = "context(kotlin/Int)"
        val cursor = Cursor(input)
        val contextParams = cursor.parseContextAndReceiverParams()
        assertEquals(1, contextParams.size)
        assertEquals("kotlin/Int", contextParams.single().type.classNameOrTag)
    }

    @Test
    fun parseSimpleContextParamsWithNewFormatting() {
        val input = "context(kotlin/Int))."
        val cursor = Cursor(input)
        val contextParams = cursor.parseContextAndReceiverParams()
        assertEquals(1, contextParams.size)
        assertEquals("kotlin/Int", contextParams.single().type.classNameOrTag)
    }

    @Test
    fun parseContextWithMultipleParams() {
        val input = "(context(kotlin/Int, kotlin.collections/List<kotlin/String>)"
        val cursor = Cursor(input)
        val contextParams = cursor.parseContextAndReceiverParams()
        assertEquals(2, contextParams.size)
        assertEquals("kotlin/Int", contextParams.first().type.classNameOrTag)
        assertEquals("kotlin.collections/List", contextParams.last().type.classNameOrTag)
        assertEquals("kotlin/String", contextParams.last().type.arguments?.first()?.type?.classNameOrTag)
    }

    @Test
    fun parseContextWithMultipleParamsAndNewFormatting() {
        val input = "(context(kotlin/Int, kotlin.collections/List<kotlin/String>))"
        val cursor = Cursor(input)
        val contextParams = cursor.parseContextAndReceiverParams()
        assertEquals(2, contextParams.size)
        assertEquals("kotlin/Int", contextParams.first().type.classNameOrTag)
        assertEquals("kotlin.collections/List", contextParams.last().type.classNameOrTag)
        assertEquals("kotlin/String", contextParams.last().type.arguments?.first()?.type?.classNameOrTag)
    }

    @Test
    fun parseContextParamsForGetter() {
        val input = "context(kotlin/Int), kotlin/Int).<get-regularValProperty>()"
        val cursor = Cursor(input)
        val contextParams = cursor.parseContextAndReceiverParams()
        assertEquals(1, contextParams.size)
    }

    @Test
    fun parseContextParamsAndReceiverInOldFormat() {
        val input = "context(kotlin/Int, kotlin/String) (kotlin/Long).example()"
        val cursor = Cursor(input)
        val contextAndReceiverParams = cursor.parseContextAndReceiverParams()
        assertEquals(3, contextAndReceiverParams.size)
        assertEquals(2, contextAndReceiverParams.count { it.kind == AbiValueParameterKind.CONTEXT} )
        assertEquals(1, contextAndReceiverParams.count { it.kind == AbiValueParameterKind.EXTENSION_RECEIVER} )
    }

    @Test
    fun parseContextParamsAndReceiverInNewFormat() {
        val input = "(context(kotlin/Int, kotlin/String), kotlin/Long).example()"
        val cursor = Cursor(input)
        val contextAndReceiverParams = cursor.parseContextAndReceiverParams()
        assertEquals(3, contextAndReceiverParams.size)
        assertEquals(2, contextAndReceiverParams.count { it.kind == AbiValueParameterKind.CONTEXT} )
        assertEquals(1, contextAndReceiverParams.count { it.kind == AbiValueParameterKind.EXTENSION_RECEIVER} )
    }


    @Test
    fun parseValueParamCrossinlineDefault() {
        val input = "crossinline kotlin/Function2<#A, #B, kotlin/Int> =..."
        val cursor = Cursor(input)
        val valueParam = cursor.parseValueParameter()!!
        assertEquals("kotlin/Function2", valueParam.type.className.toString())
        assertTrue(valueParam.hasDefaultArg)
        assertTrue(valueParam.isCrossinline)
        assertFalse(valueParam.isVararg)
    }

    @Test
    fun parseValueParamVararg() {
        val input = "kotlin/Array<out kotlin/Pair<#A, #B>>..."
        val cursor = Cursor(input)
        val valueParam = cursor.parseValueParameter()!!
        assertEquals("kotlin/Array", valueParam.type.className.toString())
        assertFalse(valueParam.hasDefaultArg)
        assertFalse(valueParam.isCrossinline)
        assertTrue(valueParam.isVararg)
    }

    @Test
    fun parseValueParametersWithTypeArgs() {
        val input = "kotlin/Array<out #A>..."
        val cursor = Cursor(input)
        val valueParam = cursor.parseValueParameter()!!
        assertEquals(1, valueParam.type.arguments!!.size)
    }

    @Test
    fun parseValueParametersWithTwoTypeArgs() {
        val input = "kotlin/Function1<kotlin/Double, kotlin/Boolean>)"
        val cursor = Cursor(input)
        val valueParam = cursor.parseValueParameter()!!
        assertEquals(2, valueParam.type.arguments!!.size)
    }

    @Test
    fun parseValueParametersEmpty() {
        val input = "() thing"
        val cursor = Cursor(input)
        val params = cursor.parseValueParameters(AbiValueParameterKind.CONTEXT)
        assertEquals(emptyList<AbiValueParameter>(), params)
        assertEquals("thing", cursor.currentLine)
    }

    @Test
    fun parseValueParamsSimple() {
        val input = "(kotlin/Function1<#A, kotlin/Boolean>)"
        val cursor = Cursor(input)
        val valueParams = cursor.parseValueParameters(AbiValueParameterKind.CONTEXT)
        assertEquals(1, valueParams!!.size)
    }

    @Test
    fun parseValueParamsTwoArgs() {
        val input = "(#A1, kotlin/Function2<#A1, #A, #A1>)"
        val cursor = Cursor(input)
        val valueParams = cursor.parseValueParameters(AbiValueParameterKind.CONTEXT)!!
        assertEquals(2, valueParams.size)
        assertEquals("A1", valueParams.first().type.tag)
    }

    @Test
    fun parseValueParamsWithHasDefaultArg() {
        val input = "(kotlin/Int =...)"
        val cursor = Cursor(input)
        val valueParams = cursor.parseValueParameters(AbiValueParameterKind.CONTEXT)
        assertEquals(1, valueParams!!.size)
        assertTrue(valueParams.single().hasDefaultArg)
    }

    @Test
    fun parseValueParamsComplex2() {
        val input =
            "(kotlin/Int, crossinline kotlin/Function2<#A, #B, kotlin/Int> =..., crossinline kotlin/Function1<#A, #B?> =..., crossinline kotlin/Function4<kotlin/Boolean, #A, #B, #B?, kotlin/Unit> =...)"
        val cursor = Cursor(input)
        val valueParams = cursor.parseValueParameters(AbiValueParameterKind.CONTEXT)!!
        assertEquals(4, valueParams.size)
        assertEquals("kotlin/Int", valueParams.first().type.classNameOrTag)
        val rest = valueParams.subList(1, valueParams.size)
        assertEquals(3, rest.size)
        assertTrue(rest.all { it.hasDefaultArg })
        assertTrue(rest.all { it.isCrossinline })
    }

    @Test
    fun parseValueParamsComplex3() {
        val input = "(kotlin/Array<out kotlin/Pair<#A, #B>>...)"
        val cursor = Cursor(input)
        val valueParams = cursor.parseValueParameters(AbiValueParameterKind.CONTEXT)!!
        assertEquals(1, valueParams.size)

        assertTrue(valueParams.single().isVararg)
        val type = valueParams.single().type
        assertEquals("kotlin/Array", type.className.toString())
    }

    @Test
    fun parseValueParamsWithStarTypeParam() {
        val input = "(my.lib/MyClass.Example<*>)"
        val cursor = Cursor(input)
        val valueParams = cursor.parseValueParameters(AbiValueParameterKind.CONTEXT)!!
        assertEquals(1, valueParams.size)
        val valueParam = valueParams.single()
        val type = valueParam.type
        assertEquals("my.lib/MyClass.Example", type.className.toString())
        assertEquals(1, type.arguments!!.size)
        assertInstanceOf(AbiTypeArgument.StarProjection::class.java, type.arguments?.single())
    }

    @Test
    fun parseTypeParams() {
        val input = "<#A1: kotlin/Any?>"
        val cursor = Cursor(input)
        val typeParams = cursor.parseTypeParams()!!
        assertEquals(1, typeParams.size)
        val type = typeParams.single().upperBounds.single()
        assertEquals("A1", typeParams.single().tag)
        assertEquals("kotlin/Any", type.className.toString())
        assertEquals(AbiTypeNullability.MARKED_NULLABLE, type.nullability)
        assertEquals(AbiVariance.INVARIANT, typeParams.single().variance)
    }

    @Test
    fun parseTypeParamsWithVariance() {
        val input = "<#A1: out kotlin/Any?>"
        val cursor = Cursor(input)
        val typeParams = cursor.parseTypeParams()!!
        assertEquals(1, typeParams.size)
        val type = typeParams.single().upperBounds.single()
        assertEquals("A1", typeParams.single().tag)
        assertEquals("kotlin/Any", type.classNameOrTag)
        assertEquals(AbiTypeNullability.MARKED_NULLABLE, type.nullability)
        assertEquals(AbiVariance.OUT, typeParams.single().variance)
    }

    @Test
    fun parseTypeParamsWithTwo() {
        val input = "<#A: kotlin/Any?, #B: kotlin/Any?>"
        val cursor = Cursor(input)
        val typeParams = cursor.parseTypeParams()!!
        assertEquals(2, typeParams.size)
        val type1 = typeParams.first().upperBounds.single()
        val type2 = typeParams.first().upperBounds.single()
        assertEquals("A", typeParams.first().tag)
        assertEquals("B", typeParams.last().tag)
        assertEquals("kotlin/Any", type1.classNameOrTag)
        assertEquals(AbiTypeNullability.MARKED_NULLABLE, type1.nullability)
        assertEquals("kotlin/Any", type2.classNameOrTag)
        assertEquals(AbiTypeNullability.MARKED_NULLABLE, type2.nullability)
    }

    @Test
    fun parseTypeParamsWithMultipleUpperBounds() {
        val input = "<#A: my.lib/A & my.lib/B>"
        val cursor = Cursor(input)
        val typeParams = cursor.parseTypeParams()!!
        assertEquals(1, typeParams.size)
        val upperBounds = typeParams.single().upperBounds
        assertEquals(2, upperBounds.size)
        assertEquals("my.lib/A", upperBounds.first().classNameOrTag)
        assertEquals("my.lib/B", upperBounds.last().classNameOrTag)
    }

    @Test
    fun parseTypeParamsIsReified() {
        val input = "<#A1: reified kotlin/Any?>"
        val cursor = Cursor(input)
        val typeParam = cursor.parseTypeParams()?.single()
        assertNotNull(typeParam)
        assertTrue(typeParam!!.isReified)
    }

    @Test
    fun parseTypeParamsDoesNotMatchGetter() {
        val input = "<get-size>"
        val cursor = Cursor(input)
        val typeParams = cursor.parseTypeParams()
        assertNull(typeParams)
    }

    @Test
    fun parseTypeArgs() {
        val input = "<out #A>"
        val cursor = Cursor(input)
        val typeArgs = cursor.parseTypeArgs()
        assertEquals(1, typeArgs!!.size)
        val typeArg = typeArgs.single()
        assertEquals("A", typeArg.type?.tag)
        assertEquals(AbiVariance.OUT, typeArg.variance)
    }

    @Test
    fun parseTwoTypeArgs() {
        val input = "<kotlin/Double, kotlin/Boolean>"
        val cursor = Cursor(input)
        val typeArgs = cursor.parseTypeArgs()
        assertEquals(2, typeArgs!!.size)
        assertEquals("kotlin/Double", typeArgs.first().type!!.classNameOrTag)
        assertEquals("kotlin/Boolean", typeArgs.last().type!!.classNameOrTag)
    }

    @Test
    fun parseTypeArgsWithNestedBrackets() {
        val input =
            "<my.lib/Class<#A, #B>, my.lib/Other<#A, #B>>, something else"
        val cursor = Cursor(input)
        val typeArgs = cursor.parseTypeArgs()
        assertEquals(2, typeArgs!!.size)
        assertEquals(", something else", cursor.currentLine)
    }

    @Test
    fun parseVarargSymbol() {
        val input = "..."
        val cursor = Cursor(input)
        val vararg = cursor.parseVarargSymbol()
        assertNotNull(vararg)
    }

    @Test
    fun hasSignatureVersion() {
        val input = "- Signature version: 2"
        val cursor = Cursor(input)
        assertTrue(cursor.hasSignatureVersion())
        assertEquals(input, cursor.currentLine)
    }

    @Test
    fun hasSignatureVersionFalsePositive() {
        val input = "// - Show manifest properties: true"
        val cursor = Cursor(input)
        assertFalse(cursor.hasSignatureVersion())
    }

    @Test
    fun parseSignatureVersion() {
        val input = "- Signature version: 2"
        val cursor = Cursor(input)
        val signatureVersion = cursor.parseSignatureVersion()!!
        assertTrue(signatureVersion.isSupportedByAbiReader)
        assertEquals(2, signatureVersion.versionNumber)
    }

    @Test
    fun parseInvalidSignatureVersion() {
        val input = "- Signature version: 101"
        val cursor = Cursor(input)
        val signatureVersion = cursor.parseSignatureVersion()!!
        assertFalse(signatureVersion.isSupportedByAbiReader)
        assertEquals(101, signatureVersion.versionNumber)
    }

    @Test
    fun parseSimpleEnumEntryName() {
        val input = "SOME_ENUM"
        val cursor = Cursor(input)
        val enumName = cursor.parseEnumEntryName()
        assertEquals("SOME_ENUM", enumName)
    }

    @Test
    fun parseComplexEnumEntryName() {
        val input = "sOME! enum? //"
        val cursor = Cursor(input)
        val enumName = cursor.parseEnumEntryName()
        assertEquals("sOME! enum? ", enumName)
        assertEquals("//", cursor.currentLine)
    }

    @Test
    fun parseEnumEntryKind() {
        val input = "enum entry SOME ENUM! // my.lib/MyEnum.ONE|null[0]"
        val cursor = Cursor(input)
        val enumName = cursor.parseEnumEntryKind()
        assertEquals("enum entry", enumName)
        assertEquals("SOME ENUM! // my.lib/MyEnum.ONE|null[0]", cursor.currentLine)
    }

    @Test
    fun hasEnumEntry() {
        val input = "enum entry SOME_ENUM"
        val cursor = Cursor(input)
        assertTrue(cursor.hasEnumEntry())
        assertEquals(input, cursor.currentLine)
    }

    @Test
    fun parseValidIdentifierAndMaybeTrimForFunctionName() {
        val input = "myFun ()"
        val cursor = Cursor(input)
        assertEquals("myFun ", cursor.parseValidIdentifierAndMaybeTrim())
    }

    @Test
    fun parseValidIdentifierAndMaybeTrimForClassName() {
        val input = "MyClass  {"
        val cursor = Cursor(input)
        assertEquals("MyClass ", cursor.parseValidIdentifierAndMaybeTrim())
    }

    @Test
    fun parseValidIdentifierAndMaybeTrimForClassWithSuper() {
        val input = "MyClass = : my.lib/MyClass {"
        val cursor = Cursor(input)
        assertEquals("MyClass =", cursor.parseValidIdentifierAndMaybeTrim())
    }

    @Test
    fun parseValidIdentifierAndMaybeTrimForForValueParameter() {
        val input = "MyClass ,"
        val cursor = Cursor(input)
        assertEquals("MyClass ", cursor.parseValidIdentifierAndMaybeTrim())
    }
}
