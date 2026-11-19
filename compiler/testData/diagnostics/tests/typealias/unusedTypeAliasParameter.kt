// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: +UNUSED_TYPEALIAS_PARAMETER
// some parameters are unused
typealias Test<T, <!UNUSED_TYPEALIAS_PARAMETER!>X<!>> = List<T>
// X is passed to Test, but Test drops it after expansion
typealias Test2<T, <!UNUSED_TYPEALIAS_PARAMETER!>X<!>> = Test<T, X>
//all parameters are used
typealias Test3<T> = List<T>
//no parameters are used
typealias B<<!UNUSED_TYPEALIAS_PARAMETER!>T<!>> = String
// X is dropped by a type alias nested in a type argument
typealias Test4<T, <!UNUSED_TYPEALIAS_PARAMETER!>X<!>> = List<Test<T, X>>

/* GENERATED_FIR_TAGS: nullableType, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
