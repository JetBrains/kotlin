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

import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertNotNull
import kotlin.test.assertFailsWith
import org.jetbrains.kotlin.library.abi.AbiClass
import org.jetbrains.kotlin.library.abi.AbiClassKind
import org.jetbrains.kotlin.library.abi.AbiCompoundName
import org.jetbrains.kotlin.library.abi.AbiEnumEntry
import org.jetbrains.kotlin.library.abi.AbiFunction
import org.jetbrains.kotlin.library.abi.AbiModality
import org.jetbrains.kotlin.library.abi.AbiProperty
import org.jetbrains.kotlin.library.abi.AbiQualifiedName
import org.jetbrains.kotlin.library.abi.AbiSignatureVersion
import org.jetbrains.kotlin.library.abi.AbiTypeArgument
import org.jetbrains.kotlin.library.abi.AbiValueParameter
import org.jetbrains.kotlin.library.abi.AbiValueParameterKind
import org.jetbrains.kotlin.library.abi.ExperimentalLibraryAbiReader
import org.jetbrains.kotlin.library.abi.LibraryAbi
import org.junit.Ignore
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertIterableEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertThrows
import java.text.ParseException
import kotlin.toString

class KlibDumpParserTest {

    @Test
    fun parseASimpleClass() {
        val input =
            "final class my.lib/Child : my.lib/Parent"
        val parsed = KlibDumpParser(input).parseClass()
        assertNotNull(parsed)

        assertEquals("my.lib/Child", parsed.qualifiedName.toString())
    }

    @Test
    fun parseAClassWithTwoSuperTypes() {
        val input =
            "final class my.lib/ArraySet : kotlin.collections/MutableCollection<#A>, kotlin.collections/MutableSet<#A>"
        val parsed = KlibDumpParser(input).parseClass()
        assertNotNull(parsed)

        assertEquals("my.lib/ArraySet", parsed.qualifiedName.toString())
        assertEquals(2, parsed.superTypes.size)
    }

    @Test
    fun parseAClassWithTypeParams() {
        val input =
            "final class <#A: kotlin/Any?, #B: kotlin/Any?> my.lib/Child : my.lib/Parent<#A, #B>"
        val parsed = KlibDumpParser(input).parseClass()
        assertNotNull(parsed)

        assertEquals("my.lib/Child", parsed.qualifiedName.toString())
        assertEquals(2, parsed.typeParameters.size)
        parsed.typeParameters.forEach {
            assertEquals("kotlin/Any", it.upperBounds.single().className?.toString())
        }
        assertEquals("A", parsed.typeParameters.first().tag)
        assertEquals("B", parsed.typeParameters.last().tag)
    }

    @Test
    fun parseAClassWithATypeArg() {
        val input = "final class my.lib/MySubClass : my.lib/MyClass<kotlin/Int>"
        val parsed = KlibDumpParser(input).parseClass()

        assertEquals(parsed.typeParameters.size, 0)
        assertEquals(1, parsed.superTypes.size)
        val superType = parsed.superTypes.single()
        assertEquals("kotlin/Int", superType.arguments?.single()?.type?.classNameOrTag)
    }

    @Test
    fun parseAClassBWithProperties() {
        val input =
            """
            abstract interface example.lib/MutablePoint { // example.lib/MutablePoint|null[0]
                abstract var x // example.lib/MutablePoint.x|{}x[0]
                    abstract fun <get-x>(): kotlin/Float // example.lib/MutablePoint.x.<get-x>|<get-x>(){}[0]
                    abstract fun <set-x>(kotlin/Float) // example.lib/MutablePoint.x.<set-x>|<set-x>(kotlin.Float){}[0]
                abstract var y // example.lib/MutablePoint.y|{}y[0]
                    abstract fun <get-y>(): kotlin/Float // example.lib/MutablePoint.y.<get-y>|<get-y>(){}[0]
                    abstract fun <set-y>(kotlin/Float) // example.lib/MutablePoint.y.<set-y>|<set-y>(kotlin.Float){}[0]
            }
        """
                .trimIndent()
        val parsed = KlibDumpParser(input).parseClass()
        assertEquals(2, parsed.declarations.filterIsInstance<AbiProperty>().size)
    }

    @Test
    fun parseAnAnnotationClass() {
        val input = "open annotation class my.lib/MyClass : kotlin/Annotation"
        val parsed = KlibDumpParser(input).parseClass()
        assertNotNull(parsed)

        assertEquals("my.lib/MyClass", parsed.qualifiedName.toString())
        assertEquals(AbiClassKind.ANNOTATION_CLASS, parsed.kind)
    }

    @Test
    fun parseASerializerClass() {
        val input =
            """
                final object ${'$'}serializer : kotlinx.serialization.internal/GeneratedSerializer<example.lib/MyClass> { // example.lib/MyClass.${'$'}serializer|null[0]
                    final val descriptor // example.lib/MyClass.${'$'}serializer.descriptor|{}descriptor[0]
                        final fun <get-descriptor>(): kotlinx.serialization.descriptors/SerialDescriptor // example.lib/MyClass.${'$'}serializer.descriptor.<get-descriptor>|<get-descriptor>(){}[0]

                    final fun childSerializers(): kotlin/Array<kotlinx.serialization/KSerializer<*>> // example.lib/MyClass.${'$'}serializer.childSerializers|childSerializers(){}[0]
                    final fun deserialize(kotlinx.serialization.encoding/Decoder): example.lib/MyClass // example.lib/MyClass.${'$'}serializer.deserialize|deserialize(kotlinx.serialization.encoding.Decoder){}[0]
                    final fun serialize(kotlinx.serialization.encoding/Encoder, example.lib/MyClass) // example.lib/MyClass.${'$'}serializer.serialize|serialize(kotlinx.serialization.encoding.Encoder;example.lib.MyClass){}[0]
                }
        """
                .trimIndent()
        val parsed =
            KlibDumpParser(input)
                .parseClass(
                    AbiQualifiedName(
                        AbiCompoundName("example.lib"),
                        AbiCompoundName("MyClass"),
                    )
                )
        assertNotNull(parsed)
        assertEquals("example.lib/MyClass.\$serializer", parsed.qualifiedName.toString())
    }

    @Test
    fun parseAClassWithContextAndReceiverOnAGetter() {
        val input = """
        final class properties_with_context_parameters.test/FunctionContainer { // properties_with_context_parameters.test/FunctionContainer|null[0]
            final val regularValProperty // properties_with_context_parameters.test/FunctionContainer.regularValProperty|(kotlin.Int){}regularValProperty[0]
                final fun (context(kotlin/Int)).<get-regularValProperty>(): kotlin/String // properties_with_context_parameters.test/FunctionContainer.regularValProperty.<get-regularValProperty>|<get-regularValProperty>(kotlin.Int)(){}[0]
        }
        """.trimIndent()
        val parsed = KlibDumpParser(input).parseClass()
        assertNotNull(parsed)
    }
    @Test
    fun parseASimpleFunction() {
        val input =
            "final fun myFun(kotlin/Int): kotlin/String"
        val parentQName =
            AbiQualifiedName(AbiCompoundName("my.lib"), AbiCompoundName("MyClass"))
        val parsed = KlibDumpParser(input).parseFunction(parentQName)
        assertNotNull(parsed)

        assertEquals("my.lib/MyClass.myFun", parsed.qualifiedName.toString())
        assertEquals("kotlin/String", parsed.returnType!!.classNameOrTag)
        assertEquals(1, parsed.valueParameters.size)
        assertEquals("kotlin/Int", parsed.valueParameters.single().type.classNameOrTag)
    }

    @Test
    fun parseAFunction() {
        val input =
            "final inline fun <#A1: kotlin/Any?> myFun(#A1, kotlin/Function<#A1, #A, #A1>): #A1"
        val parentQName =
            AbiQualifiedName(AbiCompoundName("my.lib"), AbiCompoundName("MyClass"))
        val parsed = KlibDumpParser(input).parseFunction(parentQName)
        assertNotNull(parsed)

        assertEquals("my.lib/MyClass.myFun", parsed.qualifiedName.toString())

        assertEquals(1, parsed.typeParameters.size)
        assertEquals("A1", parsed.typeParameters.single().tag)

        assertEquals("A1", parsed.returnType!!.classNameOrTag)

        assertEquals(2, parsed.valueParameters.size)
        assertEquals("A1", parsed.valueParameters.first().type.classNameOrTag)
        assertEquals("kotlin/Function", parsed.valueParameters.last().type.classNameOrTag)
    }

    @Test
    fun parseAFunctionWithTypeArgsOnParams() {
        val input =
            "final fun <#A: kotlin/Any?> my.lib/arraySetOf(kotlin/Array<out #A>...): my.lib/ArraySet<#A>"
        val parsed = KlibDumpParser(input).parseFunction()
        assertNotNull(parsed)

        assertEquals("my.lib/arraySetOf", parsed.qualifiedName.toString())
        val param = parsed.valueParameters.single()
        assertNotEquals(param.type.arguments?.size, 0)
    }

    @Test
    fun parseAFunctionWithQualifiedReceiver() {
        val input =
            "final fun <#A: kotlin/Any> (my.lib/MyClass.Builder.Inner).my.lib/myFunWithReceiver(my.lib/Other? = ..., kotlin/Function1<my.lib/Another.Builder, #A>): #A"
        val parsed = KlibDumpParser(input).parseFunction()
        assertNotNull(parsed)

        assertEquals("my.lib/myFunWithReceiver", parsed.qualifiedName.toString())
        assertTrue(parsed.hasExtensionReceiverParameter())
    }

    @Test
    fun parseAFunctionWithSingleContextValue() {
        val input = "final fun context(kotlin/Int) my.lib/bar()"
        val parsed = KlibDumpParser(input).parseFunction()
        assertNotNull(parsed)

        assertEquals(1, parsed.contextReceiverParametersCount())
        assertEquals("kotlin/Int", parsed.valueParameters.first().type.className.toString())
    }

    @Test
    fun parseAFunctionWithSingleContextValueWithNewFormatting() {
        val input = "final fun (context(kotlin/Int)).my.lib/bar()"
        val parsed = KlibDumpParser(input).parseFunction()
        assertNotNull(parsed)

        assertEquals(1, parsed.contextReceiverParametersCount())
        assertEquals("kotlin/Int", parsed.valueParameters.first().type.className.toString())
    }

    @Test
    fun parseAFunctionWithMultipleContextValuesAndAReceiver() {
        val input =
            "final fun context(kotlin/Int, kotlin/String) (kotlin/Int).my.lib/bar(kotlin/Double)"
        val parsed = KlibDumpParser(input).parseFunction()
        assertNotNull(parsed)

        assertEquals(2, parsed.contextReceiverParametersCount())
        assertTrue(parsed.hasExtensionReceiverParameter())
        assertEquals(listOf("kotlin/Int", "kotlin/String", "kotlin/Int", "kotlin/Double"), parsed.valueParameters.map { it.type.className.toString() })
    }

    @Test
    fun parseAFunctionWithContextParamsAndDefaultArgs() {
        val input = "final fun (context(kotlin/Int, kotlin/Long)).callables_with_context_parameters.test/funWithDefaultArgs(kotlin/Int = ..., kotlin/Long, kotlin/String = ...): kotlin/String"
        val parsed = KlibDumpParser(input).parseFunction()

        assertNotNull(parsed)
        assertEquals("callables_with_context_parameters.test/funWithDefaultArgs", parsed.qualifiedName.toString())
        assertEquals(2, parsed.contextReceiverParametersCount())
    }

    @Test
    fun parseAGetterFunction() {
        val input = "final inline fun <get-indices>(): kotlin.ranges/IntRange"
        val parentQName =
            AbiQualifiedName(AbiCompoundName("my.lib"), AbiCompoundName("ObjectList"))
        val parsed = KlibDumpParser(input).parseFunction(parentQName, isGetterOrSetter = true)
        assertEquals("my.lib/ObjectList.<get-indices>", parsed.qualifiedName.toString())
    }

    @Test
    fun parseAGetterFunctionWithReceiver() {
        val input =
            "final inline fun <#A1: kotlin/Any?> (my.lib/MyCollection<#A1>).<get-size>(): kotlin/Int"
        val parentQName =
            AbiQualifiedName(AbiCompoundName("my.lib"), AbiCompoundName("MyClass"))
        val parsed = KlibDumpParser(input).parseFunction(parentQName, isGetterOrSetter = true)
        assertEquals("my.lib/MyClass.<get-size>", parsed.qualifiedName.toString())
        assertEquals("kotlin/Int", parsed.returnType!!.classNameOrTag)
        val receiver = parsed.extensionReceiverParameter()
        assertNotNull(receiver)
        assertEquals("my.lib/MyCollection", receiver!!.type.classNameOrTag)
    }

    @Test
    fun parseAFunctionWithTypeArgAsReceiver() {
        val input =
            "final inline fun <#A: example.lib/Closeable, #B: kotlin/Any?> (#A).example.lib/use(kotlin/Function1<#A, #B>): #B"
        val parsed = KlibDumpParser(input).parseFunction()

        assertEquals(2, parsed.typeParameters.size)
        val receiver = parsed.extensionReceiverParameter()
        assertNotNull(receiver)
        assertEquals("A", receiver!!.type.classNameOrTag)
    }

    @Test
    fun parseAComplexFunction() {
        val input =
            "final inline fun <#A: kotlin/Any, #B: kotlin/Any> my.lib/myFun(kotlin/Int, crossinline kotlin/Function2<#A, #B, kotlin/Int> =..., crossinline kotlin/Function1<#A, #B?> =..., crossinline kotlin/Function4<kotlin/Boolean, #A, #B, #B?, kotlin/Unit> =...): my.lib/LruCache<#A, #B>"
        val parsed = KlibDumpParser(input).parseFunction()
        assertEquals(AbiModality.FINAL, parsed.modality)
        assertEquals(2, parsed.typeParameters.size)
        assertEquals("my.lib/myFun", parsed.qualifiedName.toString())
        assertEquals(4, parsed.valueParameters.size)
    }

    @Test
    fun parseAComplexFunctionWithK2Formatting() {
        val input =
            "final inline fun <#A: kotlin/Any, #B: kotlin/Any> my.lib/lruCache(kotlin/Int, crossinline kotlin/Function2<#A, #B, kotlin/Int> = ..., crossinline kotlin/Function1<#A, #B?> = ..., crossinline kotlin/Function4<kotlin/Boolean, #A, #B, #B?, kotlin/Unit> = ...): my.lib/LruCache<#A, #B>"
        val parsed = KlibDumpParser(input).parseFunction()
        assertEquals(AbiModality.FINAL, parsed.modality)
        assertEquals(2, parsed.typeParameters.size)
        assertEquals("my.lib/lruCache", parsed.qualifiedName.toString())
        assertEquals(4, parsed.valueParameters.size)
    }

    @Test
    fun parseAPropertyWithTheWordContextInIt() {
        val input =
            """
            final val my.lib/MyProp
                final fun <get-MyProp>(): my.other.lib/SomethingElse<my.lib/AnotherThing?>
        """
                .trimIndent()
        val parsed = KlibDumpParser(input).parseProperty()
        assertEquals("my.lib/MyProp", parsed.qualifiedName.toString())
    }

    @Test
    fun parseANestedValProperty() {
        val input = """
            final val size
                final fun <get-size>(): kotlin/Int
        """.trimIndent()
        val parsed =
            KlibDumpParser(input)
                .parseProperty(
                    AbiQualifiedName(
                        AbiCompoundName("my.lib"),
                        AbiCompoundName("ScatterMap"),
                    )
                )
        assertNotNull(parsed.getter)
        assertNull(parsed.setter)
    }

    @Test
    fun parseAVarProperty() {
        val input = """
            final var examples
                final fun <get-examples>(): kotlin/Array<kotlin/Any?>
                final fun <set-examples>(kotlin/Array<kotlin/Any?>)
        """.trimIndent()
        val parsed =
            KlibDumpParser(input)
                .parseProperty(
                    AbiQualifiedName(
                        AbiCompoundName("my.lib"),
                        AbiCompoundName("MyClass"),
                    )
                )
        assertEquals("my.lib/MyClass.examples", parsed.qualifiedName.toString())
        assertEquals("my.lib/MyClass.examples.<get-examples>", parsed.getter!!.qualifiedName.toString())
        assertEquals("my.lib/MyClass.examples.<set-examples>", parsed.setter!!.qualifiedName.toString())
        assertEquals("kotlin/Array", parsed.getter!!.returnType!!.classNameOrTag)
        assertEquals("kotlin/Array", parsed.setter!!.valueParameters.single().type.classNameOrTag)
    }

    @Test
    fun parseAPropertyWithStarParamsInReceiver() {
        val input =
            """
            final val my.lib/isFinished
                final fun (my.lib/Something<*, *>).<get-Example>(): kotlin/Boolean
        """
                .trimIndent()
        val parsed = KlibDumpParser(input).parseProperty()
        assertNotNull(parsed.getter)
        assertTrue(
            parsed.getter?.valueParameters?.any {
                it.kind == AbiValueParameterKind.EXTENSION_RECEIVER
            }!!
        )
        val receiver = parsed.getter!!.extensionReceiverParameter()!!
        assertTrue(receiver.type.arguments!!.all { it is AbiTypeArgument.StarProjection })
    }

    @Test
    fun parseAPropertyWithContextParams() {
        val input = """
            final val example/regularValProperty // example/regularValProperty|(kotlin.Int)@kotlin.Int{}regularValProperty[0]
                final fun (context(kotlin/Int), kotlin/Int).<get-regularValProperty>(): kotlin/String // example/regularValProperty.<get-regularValProperty>|<get-regularValProperty>(kotlin.Int)@kotlin.Int(){}[0]
        """.trimIndent()
        val parsed = KlibDumpParser(input).parseProperty()
        assertNotNull(parsed.getter)
    }

    @Test
    fun parseAnEnumEntry() {
        val input = "enum entry MY_ENUM"
        val parsed =
            KlibDumpParser(input)
                .parseEnumEntry(
                    AbiQualifiedName(
                        AbiCompoundName("my.lib"),
                        AbiCompoundName("MyEnumClass"),
                    )
                )
        assertEquals("my.lib/MyEnumClass.MY_ENUM", parsed.qualifiedName.toString())
    }

    @Test
    fun parseAnInvalidDeclaration() {
        val input =
            """
            final class my.lib/MyClass {
                invalid
            }
        """
                .trimIndent()
        val e = assertFailsWith<ParseException> { KlibDumpParser(input, "current.txt").parse() }
        assertEquals("Failed to parse unknown declaration at current.txt:1:4: 'invalid'", e.message)
    }

    @Test
    fun parseSingleTopLevelDeclaration() {
        val input = "$exampleMetadata\nfinal fun my.lib/foo(kotlin/Int, kotlin/Int): kotlin/Int"
        val parsed = KlibDumpParser(input, "current.txt").parse()
        assertEquals(1, parsed.topLevelDeclarations.declarations.size)
    }

    @Test
    fun parseAConstructorWithDefaultValue() {
        val input = "constructor <init>(kotlin/Int =..., kotlin/Int =...)"
        val parsed =
            KlibDumpParser(input, "current.txt")
                .parseFunction(
                    parentQualifiedName =
                        AbiQualifiedName(
                            AbiCompoundName("my.lib"),
                            AbiCompoundName("MyClass"),
                        )
                )
        assertTrue(parsed.isConstructor)
        assertIterableEquals(listOf("kotlin/Int", "kotlin/Int"), parsed.valueParameters.map { it.type.classNameOrTag })
    }

    @Test
    fun parseAConstructorWithDefaultValueAlternate() {
        val input =
            "constructor <init>(kotlin/Int = ...) // my.lib/MyClass.<init>|<init>(kotlin.Int){}[0]"
        val parsed =
            KlibDumpParser(input, "current.txt")
                .parseFunction(
                    parentQualifiedName =
                        AbiQualifiedName(
                            AbiCompoundName("my.lib"),
                            AbiCompoundName("MyClass"),
                        )
                )
        assertTrue(parsed.isConstructor)
        assertEquals("kotlin/Int", parsed.valueParameters.single().type.classNameOrTag)
    }

    @Test
    fun parseClassNameThatEndsWithASpace() {
        val input =
            """$exampleMetadata
            open class my.lib/MyClass  { // my.lib/MyClass |null[0]
                constructor <init>() // my.lib/MyClass .<init>|<init>(){}[0]
            }
        """
                .trimIndent()
        val parsed = KlibDumpParser(input, "current.txt").parse()
        val parsedClass =
            parsed
                .topLevelDeclarations
                .declarations
                .filterIsInstance<AbiClass>()
                .single()
        assertEquals("my.lib/MyClass ", parsedClass.qualifiedName.toString())
    }

    @Test
    fun parseAVeryAnnoyingClassName() {
        val input =
            """$exampleMetadata
            final class my.lib/MyMaybeClass = =  { // my.lib/MyMaybeClass = = |null[0]
                constructor <init>() // my.lib/MyMaybeClass = = .<init>|<init>(){}[0]
            }
            final fun my.lib/foo(my.lib/MyMaybeClass = =  =...): kotlin/Int // my.lib/foo|foo(my.lib.MyMaybeClass = = ){}[0]
        """
                .trimIndent()
        val parsed = KlibDumpParser(input, "current.txt").parse()
        val parsedFunc =
            parsed
                .topLevelDeclarations
                .declarations
                .filterIsInstance<AbiFunction>()
                .single()
        val parsedClass =
            parsed
                .topLevelDeclarations
                .declarations
                .filterIsInstance<AbiClass>()
                .single()
        assertEquals("my.lib/MyMaybeClass = = ", parsedClass.qualifiedName.toString())
        assertEquals(1, parsedFunc.valueParameters.size)
        assertEquals("my.lib/MyMaybeClass = = ", parsedFunc.valueParameters.single().type.classNameOrTag)
        assertTrue(parsedFunc.valueParameters.single().hasDefaultArg)
    }

    @Test
    fun parsesSignatureVersionAndAssignsToDeclarations() {
        val input = """
            // Rendering settings:
            // - Signature version: 2
            // - Show manifest properties: true
            // - Show declarations: true
            
            // Library unique name: <example:library>
            final fun my.lib/commonFun() // my.lib/commonFun|commonFun(){}[0]
        """.trimIndent()
        val parsed = KlibDumpParser(input).parse()
        assertEquals(2, parsed.signatureVersions.single().versionNumber)
        val commonFun = parsed.topLevelDeclarations.declarations.single()
        assertEquals("v2", commonFun.signatures.get(AbiSignatureVersion.resolveByVersionNumber(2)))
    }

    @Test
    fun parseTopLevelDeclarationsFromRootPackage() {
        val input = """
            // Rendering settings:
            // - Signature version: 2
            // - Show manifest properties: true
            // - Show declarations: true
            final class /MyRootClass { // /MyRootClass|null[0]
                constructor <init>() // /MyRootClass.<init>|<init>(){}[0]
            }
            final fun /myRootFun(): kotlin/Int // /myRootFun|myRootFun(){}[0]
            final val /myRootProp // /myRootProp|{}myRootProp[0]
                final fun <get-myRootProp>(): kotlin/Int // /myRootProp.<get-myRootProp>|<get-myRootProp>(){}[0]
            """
        val parsed  = KlibDumpParser(input).parse()
        val myClass = parsed.topLevelDeclarations.declarations.filterIsInstance<AbiClass>().single()
        assertEquals("/MyRootClass", myClass.qualifiedName.toString())
        val myFun = parsed.topLevelDeclarations.declarations.filterIsInstance<AbiFunction>().single()
        assertEquals("/myRootFun", myFun.qualifiedName.toString())
        val myProp = parsed.topLevelDeclarations.declarations.filterIsInstance<AbiProperty>().single()
        assertEquals("/myRootProp", myProp.qualifiedName.toString())
    }

    @Test
    fun parseAFullDumpWithVariousTypes() {
        val input = """
            // Rendering settings:
            // - Signature version: 2
            // - Show manifest properties: true
            // - Show declarations: true
            
            // Library unique name: <example:library>
            final class <#A: kotlin/Any?> my.lib/MyClass { // my.lib/MyClass|null[0]
                constructor <init>() // my.lib/MyClass.<init>|<init>(){}[0]
                final class InnerClass { // my.lib/MyClass.InnerClass|null[0]
                    constructor <init>() // my.lib/MyClass.InnerClass.<init>|<init>(){}[0]
                    final fun <#A2: kotlin/Any?> innerFun(#A2): #A2 // my.lib/MyClass.InnerClass.innerFun|innerFun(0:0){0§<kotlin.Any?>}[0]
                }
                final fun <#A1: kotlin/Any?> myFun(#A1): #A1 // my.lib/MyClass.myFun|myFun(0:0){0§<kotlin.Any?>}[0]
                final object Companion { // my.lib/MyClass.Companion|null[0]
                    final fun static(): kotlin/Int // my.lib/MyClass.Companion.static|static(){}[0]
                    final val example // my.lib/MyClass.Companion.example|{}example[0]
                        final fun <get-example>(): kotlin/String // my.lib/MyClass.Companion.example.<get-example>|<get-example>(){}[0]
                }
            }
            final enum class my.lib/MyEnum : kotlin/Enum<my.lib/MyEnum> { // my.lib/MyEnum|null[0]
                enum entry ONE // my.lib/MyEnum.ONE|null[0]
                enum entry TWO // my.lib/MyEnum.TWO|null[0]
                final fun valueOf(kotlin/String): my.lib/MyEnum // my.lib/MyEnum.valueOf|valueOf#static(kotlin.String){}[0]
                final fun values(): kotlin/Array<my.lib/MyEnum> // my.lib/MyEnum.values|values#static(){}[0]
                final val entries // my.lib/MyEnum.entries|#static{}entries[0]
                    final fun <get-entries>(): kotlin.enums/EnumEntries<my.lib/MyEnum> // my.lib/MyEnum.entries.<get-entries>|<get-entries>#static(){}[0]
            }
            final fun my.lib/topLevel(): kotlin/Int // my.lib/topLevel|topLevel(){}[0]
            final object my.lib/MyObject // my.lib/MyObject|null[0]
            final var my.lib/myProp // my.lib/myProp|{}myProp[0]
                final fun <get-myProp>(): kotlin/String // my.lib/myProp.<get-myProp>|<get-myProp>(){}[0]
                final fun <set-myProp>(kotlin/String) // my.lib/myProp.<set-myProp>|<set-myProp>(kotlin.String){}[0]
        """.trimIndent()
        val parsed  = KlibDumpParser(input).parse()
        val myClass = parsed.topLevelDeclarations.declarations.filterIsInstance<AbiClass>().find {
            it.qualifiedName.toString() == "my.lib/MyClass"
        }
        assertNotNull(myClass)
        val myObject = parsed.topLevelDeclarations.declarations.filterIsInstance<AbiClass>().find {
            it.qualifiedName.toString() == "my.lib/MyObject"
        }

        assertNotNull(myObject)
        val myEnum = parsed.topLevelDeclarations.declarations.filterIsInstance<AbiClass>().find {
            it.qualifiedName.toString() == "my.lib/MyEnum"
        }
        assertNotNull(myEnum)
        assertEquals(2, myEnum!!.declarations.filterIsInstance<AbiEnumEntry>().size)

        val topLevelFun = parsed.topLevelDeclarations.declarations.filterIsInstance<AbiFunction>().single()
        assertNotNull(topLevelFun)
        val myProp = parsed.topLevelDeclarations.declarations.filterIsInstance<AbiProperty>().single()
        assertNotNull(myProp)

        val innerClass = myClass!!.declarations.filterIsInstance<AbiClass>().find {
            it.qualifiedName.toString() == "my.lib/MyClass.InnerClass"
        }
        assertNotNull(innerClass)

        val companion = myClass.declarations.filterIsInstance<AbiClass>().find {
            it.qualifiedName.toString() == "my.lib/MyClass.Companion"
        }
        assertNotNull(companion)

        val innerFun = innerClass!!.declarations.filterIsInstance<AbiFunction>().filterNot { it.isConstructor }.single()
        assertNotNull(innerFun)

        val companionFun = companion!!.declarations.filterIsInstance<AbiFunction>().single()
        assertNotNull(companionFun)
        val companionProp = companion.declarations.filterIsInstance<AbiProperty>().single()
        assertNotNull(companionProp)
    }

    companion object {
        private val exampleMetadata =
            """
            // Rendering settings:
            // - Signature version: 2
            // - Show manifest properties: true
            // - Show declarations: true
            // Library unique name: <example:library>
        """
                .trimIndent()
    }
}

private fun AbiFunction.hasExtensionReceiverParameter(): Boolean =
    valueParameters.any { it.kind == AbiValueParameterKind.EXTENSION_RECEIVER }

private fun AbiFunction.contextReceiverParametersCount(): Int =
    valueParameters.count { it.kind == AbiValueParameterKind.CONTEXT }

private fun AbiFunction.extensionReceiverParameter(): AbiValueParameter? =
    valueParameters.find { it.kind == AbiValueParameterKind.EXTENSION_RECEIVER }