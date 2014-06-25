//KT-2585 Code in try-finally is incorrectly marked as unreachable

fun foo(x: String): String {
    try {
        <!UNREACHABLE_CODE!>throw<!> RuntimeException()
    } finally {
        try {
        } catch (e: Exception) {
        }
        return x     // <- Wrong UNREACHABLE_CODE
    }
}