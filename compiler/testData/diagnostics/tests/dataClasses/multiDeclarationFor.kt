// LANGUAGE: +NameBasedDestructuring +DeprecateNameMismatchInShortDestructuringWithParentheses +EnableNameBasedDestructuringShortForm
// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

data class A(val x: Int, val y: String)

fun foo(arr: Array<A>) {
    for ([b, c] in arr) {
        checkSubtype<Int>(b)
        checkSubtype<String>(c)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, data, forLoop, funWithExtensionReceiver, functionDeclaration, functionalType,
infix, localProperty, nullableType, primaryConstructor, propertyDeclaration, typeParameter, typeWithExtension */
