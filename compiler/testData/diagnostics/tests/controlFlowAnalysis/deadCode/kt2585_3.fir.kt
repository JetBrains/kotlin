//KT-2585 Code in try-finally is incorrectly marked as unreachable

fun foo(x: String): String {
    try {
        throw RuntimeException() //should be marked as unreachable, but is not
    } finally {
        throw NullPointerException()
    }
}