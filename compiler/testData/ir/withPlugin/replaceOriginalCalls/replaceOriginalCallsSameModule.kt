// MODULE: main
// FILE: test.kt
fun foo_original(): String {
    error("Calls to '_original' functions should be replaced by generated functions calls")
}

fun box(): String {
    val res = foo_original()
    return if (res == "foo_generated") "OK" else res
}