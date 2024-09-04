// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValue

fun stringF(): String = ""
fun nsf(): String? = "null"

fun Any.consume(): Unit = Unit
fun String.str(): String = ""

fun elvis(e: String?): String {
    val c = nsf() ?: stringF() // used
    <!RETURN_VALUE_NOT_USED!>nsf()<!> ?: <!RETURN_VALUE_NOT_USED!>stringF()<!> // unused

    return e ?: nsf() ?: stringF()
}

fun safeCalls() {
    stringF().consume() // used
    <!RETURN_VALUE_NOT_USED!>stringF().str()<!> // unused
    nsf()?.consume() // used
    <!RETURN_VALUE_NOT_USED!>nsf()?.str()<!> // unused
}

fun notNullCall(s: String?) {
    s!! // locals are discardable, we propagate
    <!RETURN_VALUE_NOT_USED!>nsf()<!>!!
}

fun notNullCall2(s: String?) {
    val x = s!!
    val y = nsf()!!
}
