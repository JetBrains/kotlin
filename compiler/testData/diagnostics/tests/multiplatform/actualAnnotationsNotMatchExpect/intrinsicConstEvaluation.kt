// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: +IntrinsicConstEvaluation
// MODULE: m1-common
// FILE: common.kt
enum class MyEnum {
    FOO
}

annotation class Ann(val p: String)

@Ann("FOO")
expect fun matching()

@Ann("not FOO")
expect fun nonMatching()

@Ann("FOO")
expect class ClassMatching

@Ann("FOO")
expect class TypeAliasMatching

@Ann("not FOO")
expect class ClassNotMatching

@Ann("not FOO")
expect class TypeAliasNotMatching

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
@Ann(MyEnum.FOO.name)
actual fun matching() {}

@Ann(MyEnum.FOO.name)
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> fun nonMatching() {}

@Ann(MyEnum.FOO.name)
actual class ClassMatching
actual typealias TypeAliasMatching = ClassMatching

@Ann(MyEnum.FOO.name)
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> class ClassNotMatching
<!ACTUAL_ANNOTATIONS_NOT_MATCH_EXPECT!>actual<!> typealias TypeAliasNotMatching = ClassNotMatching

/* GENERATED_FIR_TAGS: actual, annotationDeclaration, enumDeclaration, enumEntry, expect, functionDeclaration,
primaryConstructor, propertyDeclaration, stringLiteral */
