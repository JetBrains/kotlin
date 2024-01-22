// ISSUE: KT-64891
// FIR_DUMP

fun <T> T.b(): Int = 10

fun <T> Int.a(it: T): Int = this

fun main() {
    5.<!FUNCTION_EXPECTED!>(::<!UNRESOLVED_REFERENCE!>b<!>)<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()
    5.<!FUNCTION_EXPECTED!>(::<!UNRESOLVED_REFERENCE!>a<!>)<!>('=').<!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()

    val c = Int::b
    5.<!FUNCTION_EXPECTED!>(c)<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()

    val f: Int.() -> Int = Int::b
    5.(f)().inv()

    5.<!FUNCTION_EXPECTED!>(Int::b)<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()

    val d = Int::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>a<!>
    5.<!FUNCTION_EXPECTED!>(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>d<!>)<!>('=').<!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()

    val e: Int.(Char) -> Int = Int::a
    5.(e)('=').inv()
}

val <T> T.x get(): Int = 10

fun rain() {
    5.<!FUNCTION_EXPECTED!>(<!UNRESOLVED_REFERENCE!>x<!>)<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()

    5.<!FUNCTION_EXPECTED!>(::<!UNRESOLVED_REFERENCE!>x<!>)<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()

    5.<!FUNCTION_EXPECTED!>(Int::x)<!>().<!DEBUG_INFO_MISSING_UNRESOLVED!>inv<!>()
}
