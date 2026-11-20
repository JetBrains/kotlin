// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// FIR_DUMP
// LANGUAGE: -CollectionLiterals
// LANGUAGE: -CollectionLiteralsBasedAnnotationResolution
// LANGUAGE: +CompanionBlocks

@file:OptIn(ExperimentalCollectionLiteralsApi::class, ExperimentalUnsignedTypes::class)

import kotlin.reflect.KClass

annotation class NestedAnno(
    vararg val uint: UInt = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>UIntArray.of()<!>,
    val ulong: ULongArray = [],
)

annotation class Anno(
    val uint: UIntArray = [],
    val ulong: ULongArray = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>ULongArray.of(42u)<!>,
    val ushort: UShortArray,
    val ubyte: UByteArray,
    val nested: NestedAnno = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>NestedAnno(*UIntArray.of(42u), ulong = [])<!>,
    val int: IntArray = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>IntArray.of(1, 2)<!>,
    val char: CharArray = [],
    val float: FloatArray,
    val boolean: BooleanArray,
    val strings: Array<String> = <!ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT!>Array.of("a")<!>,
    val classes: Array<KClass<*>>,
)

@Anno(
    ushort = [42u],
    ubyte = <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>UByteArray.of()<!>,
    nested = NestedAnno(uint = <!REDUNDANT_SPREAD_OPERATOR_IN_NAMED_FORM_IN_FUNCTION!>*<!>[], ulong = <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>ULongArray.of(42u)<!>),
    float = <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>FloatArray.of()<!>,
    boolean = [true],
    classes = <!ANNOTATION_ARGUMENT_MUST_BE_CONST!>Array.of(Int::class)<!>,
    strings = ["b"],
)
fun target() = Unit

/* GENERATED_FIR_TAGS: annotationDeclaration, annotationUseSiteTargetFile, classReference, collectionLiteral,
functionDeclaration, primaryConstructor, propertyDeclaration, unsignedLiteral */
