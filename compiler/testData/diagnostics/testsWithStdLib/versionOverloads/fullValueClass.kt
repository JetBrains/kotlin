// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +FullValueClasses
// ISSUE: KT-89523

value class Point(
    val x: Int,
    @IntroducedAt("2.0") val y: Int = 0,
)

value class SingleField(
    @IntroducedAt("2.0") val x: Int = 0,
)

@JvmInline
value class Inline(
    <!INVALID_VERSIONING_ON_VALUE_CLASS_PARAMETER!>@IntroducedAt("2.0")<!> val x: Int = 0,
)

/* GENERATED_FIR_TAGS: classDeclaration, integerLiteral, primaryConstructor, propertyDeclaration, stringLiteral, value */
