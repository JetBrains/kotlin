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
    val <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>used<!>: String
    used = stringF()
    lateinit var <!ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE!>used2<!>: String
    used2 = stringF()
}

class Inits {
    val init1 = stringF()

    val explicit: String
        get() = stringF()

    val unused: String
        get() {
            stringF()
            return ""
        }
}

fun defaultValue(param: String = stringF()) {}

fun basic() {
    val used = stringF() // used
    println(stringF()) // used
    stringF() // unused
}

fun stringConcat(): String {
    "42" // unsued
    val x = "42"
    "answer is $x" // unused
    val y = "answer is $x" // used
    return "answer is $y" // used
}

@MustUseReturnValue
class ISE: Exception()

fun throws(): Nothing {
    ISE() // unused
    throw ISE()
}

fun createE() = IllegalStateException() // used

fun throws2() {
    createE() // unused
    throw createE() // used
}

fun usesNothing() {
    throws() // should not be reported as unused
}

fun arrays() {
    val a = intArrayOf(1, 2, 3)
    arrayOf(1, 2)
}
