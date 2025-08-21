// RUN_PIPELINE_TILL: BACKEND
interface A

fun test_1(a: A?, convert: A.() -> String) {
    val s = a?.convert()
}

fun test_2(a: A, convert: A.() -> String) {
    val s = a.convert()
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, interfaceDeclaration, localProperty, nullableType,
propertyDeclaration, safeCall, typeWithExtension */
