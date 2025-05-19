// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// DIAGNOSTICS: -VARIABLE_NEVER_READ -ASSIGNED_VALUE_IS_NEVER_READ -CAN_BE_VAL_LATEINIT

@file:MustUseReturnValue
import kotlin.properties.ReadOnlyProperty

fun stringF(): String = ""

fun Any.consume(): Unit = Unit

fun returnsExp() = stringF()

fun returnsBody(): String {
    return stringF()
}

fun provideDelegate(): ReadOnlyProperty<Any?, Int> = null!!

fun returnPair(): Pair<String, String> = Pair("1", "2")

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

    val byDelegate by provideDelegate()

    val lazyDelegate: String by lazy { stringF() }
}

fun defaultValue(param: String = stringF()) {}

fun basic() {
    val used = stringF() // used
    println(stringF()) // used
    stringF() // unused
    val (destructuring, declaration) = returnPair()  //used
}

fun stringConcat(): String {
    "42" // unsued
    val x = "42"
    "answer is $x" // UNUSED_EXPRESSION because x has no side effects
    "answer is ${stringF()}" // RETURN_VALUE_NOT_USED because stringF() may have side effects
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
