// ISSUE: KT-64891
// FIR_DUMP

fun <T> T.b(): Int = 10

fun <T> Int.a(it: T): Int = this

fun main() {
    5.(<!UNRESOLVED_REFERENCE!>::<!UNRESOLVED_REFERENCE!>b<!><!>)().inv()
    5.(<!UNRESOLVED_REFERENCE!>::<!UNRESOLVED_REFERENCE!>a<!><!>)('=').inv()

    val c = Int::b
    5.(c)().inv()

    val f: Int.() -> Int = Int::b
    5.(f)().inv()

    5.(Int::b)().inv()

    val d = Int::<!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>a<!>
    5.(d)('=').inv()

    val e: Int.(Char) -> Int = Int::a
    5.(e)('=').inv()
}

val <T> T.x get(): Int = 10

fun rain() {
    5.(<!UNRESOLVED_REFERENCE!>x<!>)().inv()

    5.(<!UNRESOLVED_REFERENCE!>::<!UNRESOLVED_REFERENCE!>x<!><!>)().inv()

    <!NO_RECEIVER_ALLOWED!>5.(Int::x)()<!>.inv()
}
