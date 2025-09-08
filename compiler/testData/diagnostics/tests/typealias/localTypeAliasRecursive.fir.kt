// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_VARIABLE -UNUSED_PARAMETER -TOPLEVEL_TYPEALIASES_ONLY

fun outer() {
    <!UNSUPPORTED_FEATURE!>typealias Test1 = <!RECURSIVE_TYPEALIAS_EXPANSION!>Test1<!><!>
    <!UNSUPPORTED_FEATURE!>typealias Test2 = <!RECURSIVE_TYPEALIAS_EXPANSION!>List<Test2><!><!>
    <!UNSUPPORTED_FEATURE!>typealias Test3<T> = <!RECURSIVE_TYPEALIAS_EXPANSION!>List<Test3<T>><!><!>
}

/* GENERATED_FIR_TAGS: functionDeclaration, nullableType, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter,
typeParameter */
