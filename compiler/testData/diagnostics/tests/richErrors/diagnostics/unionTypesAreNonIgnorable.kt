// RUN_PIPELINE_TILL: CODEGEN
// RETURN_VALUE_CHECKER_MODE: FULL
// ISSUE: KT-89104

error class MyError
error class MyOtherError

fun unionWithUnit(): Unit | MyError = Unit
fun unionWithString(): String | MyError = ""
fun unionWithNullable(): String? | MyError = null
fun unionOfErrors(): MyError | MyOtherError = MyError()
fun <T> id(t: T): T = t

@IgnorableReturnValue
fun explicitlyIgnorableUnion(): String | MyError = ""

class Container {
    fun member(): String | MyError = ""
    val property: String | MyError get() = ""
}

fun coerceMe(block: () -> Unit) {
    block()
}

fun test(c: Container, u: String | MyError) {
    <!RETURN_VALUE_NOT_USED!>unionWithUnit<!>()
    <!RETURN_VALUE_NOT_USED!>unionWithString<!>()
    <!RETURN_VALUE_NOT_USED!>unionWithNullable<!>()
    <!RETURN_VALUE_NOT_USED!>unionOfErrors<!>()
    explicitlyIgnorableUnion()
    c.<!RETURN_VALUE_NOT_USED!>member<!>()
    c.<!RETURN_VALUE_NOT_USED!>property<!>
    <!RETURN_VALUE_NOT_USED!>id<!>(u)
    <!RETURN_VALUE_NOT_USED!>id<!>("")
    u

    fun local(): String | MyError = ""
    <!RETURN_VALUE_NOT_USED!>local<!>()

    val _ = unionWithString()
    val used = unionWithString()
    if (used is String) return
}

fun testCoercion() {
    coerceMe { <!RETURN_VALUE_NOT_USED_COERCION!>unionWithUnit<!>() }
    coerceMe { <!RETURN_VALUE_NOT_USED_COERCION!>unionWithString<!>() }
    coerceMe(::<!RETURN_VALUE_NOT_USED_COERCION!>unionWithUnit<!>)
    coerceMe(::<!RETURN_VALUE_NOT_USED_COERCION!>unionWithString<!>)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, functionDeclaration, functionalType, getter, ifExpression,
isExpression, lambdaLiteral, localFunction, localProperty, nullableType, propertyDeclaration, stringLiteral,
typeParameter, unnamedLocalVariable */
