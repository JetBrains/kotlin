// !DIAGNOSTICS: -UNUSED_VARIABLE
suspend fun foo() {}

class A {
    suspend fun member() {}
}

suspend fun A.ext() {}

fun test1(a: A) {
    val x = ::<!ILLEGAL_SUSPEND_FUNCTION_CALL, UNSUPPORTED!>foo<!>

    val y1 = a::<!ILLEGAL_SUSPEND_FUNCTION_CALL, UNSUPPORTED!>member<!>
    val y2 = A::<!ILLEGAL_SUSPEND_FUNCTION_CALL, UNSUPPORTED!>member<!>

    val z1 = a::<!ILLEGAL_SUSPEND_FUNCTION_CALL, UNSUPPORTED!>ext<!>
    val z2 = A::<!ILLEGAL_SUSPEND_FUNCTION_CALL, UNSUPPORTED!>ext<!>
}

suspend fun test2(a: A) {
    val x = ::<!UNSUPPORTED!>foo<!>

    val y1 = a::<!UNSUPPORTED!>member<!>
    val y2 = A::<!UNSUPPORTED!>member<!>

    val z1 = a::<!UNSUPPORTED!>ext<!>
    val z2 = A::<!UNSUPPORTED!>ext<!>
}
