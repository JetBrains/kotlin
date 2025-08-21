// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

package a

import checkSubtype

fun <T> emptyList(): List<T> = throw Exception()

fun <T> foo(f: T.() -> Unit, l: List<T>): T = throw Exception("$f$l")

fun test() {
    val q = foo(fun Int.() {}, emptyList()) //type inference no information for parameter error
    checkSubtype<Int>(q)

    <!CANNOT_INFER_PARAMETER_TYPE!>foo<!>(<!CANNOT_INFER_PARAMETER_TYPE!>{}<!>, <!CANNOT_INFER_PARAMETER_TYPE!>emptyList<!>())
}

/* GENERATED_FIR_TAGS: anonymousFunction, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, lambdaLiteral, localProperty, nullableType, propertyDeclaration, typeParameter, typeWithExtension */
