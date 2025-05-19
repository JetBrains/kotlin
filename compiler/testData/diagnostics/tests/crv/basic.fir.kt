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

    val byDelegate by provideDelegate()

    val lazyDelegate: String by lazy { stringF() }
}

fun defaultValue(param: String = stringF()) {}

fun basic() {
    val used = stringF() // used
    println(stringF()) // used
    <!RETURN_VALUE_NOT_USED!>stringF()<!> // unused
    val (destructuring, declaration) = returnPair()  //used
}

fun stringConcat(): String {
    <!UNUSED_EXPRESSION!>"42"<!> // unsued
    val x = "42"
    <!UNUSED_EXPRESSION!>"answer is $x"<!> // UNUSED_EXPRESSION because x has no side effects
    <!RETURN_VALUE_NOT_USED!>"answer is ${stringF()}"<!> // RETURN_VALUE_NOT_USED because stringF() may have side effects
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
