// MODULE: libA
// FILE: libA.kt

fun libA_foo_original(): String {
    error("Calls to '_original' functions should be replaced by generated functions calls")
}

// MODULE: main(libA)
// FILE: test.kt

fun box(): String {
    val res = libA_foo_original()
    return if (res == "libA_foo_generated") "OK" else res
}