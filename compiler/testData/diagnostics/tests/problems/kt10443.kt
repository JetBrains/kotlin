// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-10443

// KT-10443: Missing type inference error on variable setter
class Y<T>(var t: T)

var <T> Y<T>.proxyT: T
    get() = t
    set(value) { t = value }

fun <T> Y<T>.setter(value: T) {
    t = value
}

fun test2(y: Y<out Number>) {
    y.t <!ASSIGNMENT_TYPE_MISMATCH!>=<!> 5 // error as expected
    y.setter(<!MEMBER_PROJECTED_OUT!>5<!>) // type inference error
    y.<!SETTER_PROJECTED_OUT!>proxyT<!> = 5 // no error but should be
}

/* GENERATED_FIR_TAGS: assignment, capturedType, classDeclaration, funWithExtensionReceiver, functionDeclaration, getter,
integerLiteral, nullableType, outProjection, primaryConstructor, propertyDeclaration, propertyWithExtensionReceiver,
setter, typeParameter */
