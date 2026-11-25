// RUN_PIPELINE_TILL: CODEGEN
error class E

typealias ListRes<T> = List<T> | E
typealias LR<T> = List<T>

fun test(l: List<String> | E, a: Any, ls: List<String>) {
    l as ListRes
    ls <!USELESS_CAST!>as LR<!>
    ls as ListRes
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, functionDeclaration, intersectionType, isExpression, nullableType,
smartcast, typeAliasDeclaration, typeAliasDeclarationWithTypeParameter, typeParameter */
