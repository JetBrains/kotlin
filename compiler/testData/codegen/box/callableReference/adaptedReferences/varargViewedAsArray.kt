// KT-25514 Support usage of function reference with vararg where function of array is expected in new inference

fun foo(x: Int, vararg y: String): String = y[0]

fun useArray(f: (Int, Array<String>) -> String): String = f(1, Array(1) { "OK" })

fun box(): String {
    return useArray(::foo)
}
