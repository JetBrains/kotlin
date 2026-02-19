// LANGUAGE: -NameBasedDestructuring -DeprecateNameMismatchInShortDestructuringWithParentheses -EnableNameBasedDestructuringShortForm
// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

operator fun Int.component1() = "a"

fun foo(a: Number) {
    val (x) = a as Int
    checkSubtype<Int>(<!DEBUG_INFO_SMARTCAST!>a<!>)
    checkSubtype<String>(x)
}

/* GENERATED_FIR_TAGS: asExpression, classDeclaration, destructuringDeclaration, funWithExtensionReceiver,
functionDeclaration, functionalType, infix, localProperty, nullableType, operator, propertyDeclaration, smartcast,
stringLiteral, typeParameter, typeWithExtension */
