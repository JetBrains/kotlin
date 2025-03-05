// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

@file:MustUseReturnValue

fun getAny(): Any = ""
fun Any.consume(): Unit = Unit

fun intF(): Int = 42

val nonLocal: Any get() = ""

fun consume(s: String) {}

fun locals(a: Any) {
    <!UNUSED_EXPRESSION!>a<!>
    a as String
    val b: Any = ""
    <!UNUSED_EXPRESSION!>b<!>
    b as String
}

fun nonLocals() {
    <!RETURN_VALUE_NOT_USED!>nonLocal<!>
    <!RETURN_VALUE_NOT_USED!>nonLocal<!> as String
    val used = nonLocal as String
    consume(nonLocal as String)
    (nonLocal as String).consume()
}

fun classRefs(instance: Any) {
    <!UNUSED_EXPRESSION!>instance::class<!>
    <!UNUSED_EXPRESSION!>String::class<!>
    <!RETURN_VALUE_NOT_USED!>nonLocal::class<!>
    val s = String::class
    val ss = instance::class
    val sss = nonLocal::class
}

fun whenInstance() {
    val e = when (nonLocal) {
        is String -> "a"
        is Boolean -> "b"
        else -> "c"
    }

    when (nonLocal) {
        is String -> <!RETURN_VALUE_NOT_USED!>intF()<!>
        is Boolean -> <!RETURN_VALUE_NOT_USED!>getAny()<!>
        else -> <!RETURN_VALUE_NOT_USED!>nonLocal<!>
    }
}

fun castsInIf() {
    val x = if (intF() > 10) getAny() as String else ""
    val y = if (intF() > 10) {
        <!RETURN_VALUE_NOT_USED!>getAny()<!> // unused
        getAny() as String // used
    } else {
        ""
    }
}
