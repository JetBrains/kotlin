// RUN_PIPELINE_TILL: FRONTEND
// RENDER_DIAGNOSTIC_ARGUMENTS
error class E

typealias Res<T> = <!TYPEALIAS_SHOULD_EXPAND_TO_CLASS("T (of typealias Res<T>)")!>T | E<!>

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter,
typeParameter */
