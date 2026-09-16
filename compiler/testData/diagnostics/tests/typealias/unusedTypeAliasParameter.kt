// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: +UNUSED_TYPEALIAS_PARAMETER
typealias Test<T, X> = List<T>
typealias Test2<T, X> = Test<T, X>

/* GENERATED_FIR_TAGS: nullableType, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
