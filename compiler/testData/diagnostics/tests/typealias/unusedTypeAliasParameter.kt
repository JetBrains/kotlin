// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: +UNUSED_TYPEALIAS_PARAMETER
// some parameters are unused
typealias Test<T,X> = List<T>
// X is passed to Test, but Test drops it
typealias Test2<T, X> = Test<T, X>
//all parameters are used
typealias Test3<T> = List<T>
//no parameters are used
typealias B<T> = String

/* GENERATED_FIR_TAGS: nullableType, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
