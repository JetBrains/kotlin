//KT-2585 Code in try-finally is incorrectly marked as unreachable

fun foo(<!UNUSED_PARAMETER!>x<!>: String): String {
    try {
        <!UNREACHABLE_CODE!>throw<!> RuntimeException() //should be marked as unreachable, but is not
    } finally {
        throw NullPointerException()
    }
}