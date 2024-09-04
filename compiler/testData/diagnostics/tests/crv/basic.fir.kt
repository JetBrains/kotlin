// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// DIAGNOSTICS: -VARIABLE_NEVER_READ -ASSIGNED_VALUE_IS_NEVER_READ -CAN_BE_VAL_LATEINIT

@file:MustUseReturnValue

fun stringF(): String = ""

fun Any.consume(): Unit = Unit

fun returnsExp() = stringF()

fun returnsBody(): String {
    return stringF()
}

fun vals() {
    val used: String
    used = stringF()
    lateinit var used2: String
    used2 = stringF()
}

class Inits {
    val init1 = stringF()

    val explicit: String
        get() = stringF()

    val unused: String
        get() {
            <!RETURN_VALUE_NOT_USED!>stringF()<!>
            return ""
        }
}

fun defaultValue(param: String = stringF()) {}

fun basic() {
    val used = stringF() // used
    println(stringF()) // used
    <!RETURN_VALUE_NOT_USED!>stringF()<!> // unused
}

fun stringConcat(): String {
    <!UNUSED_EXPRESSION!>"42"<!> // unsued
    val x = "42"
    <!UNUSED_EXPRESSION!>"answer is $x"<!> // unused
    val y = "answer is $x" // used
    return "answer is $y" // used
}

@MustUseReturnValue
class ISE: Exception()

fun throws(): Nothing {
    <!RETURN_VALUE_NOT_USED!>ISE()<!> // unused
    throw ISE()
}

fun createE() = IllegalStateException() // used

fun throws2() {
    <!RETURN_VALUE_NOT_USED!>createE()<!> // unused
    throw createE() // used
}

fun usesNothing() {
    throws() // should not be reported as unused
}

fun arrays() {
    val a = intArrayOf(1, 2, 3)
    <!RETURN_VALUE_NOT_USED!>arrayOf(1, 2)<!>
}
