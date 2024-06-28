interface A {}

fun <E> getA() = (object : A {}) as A

fun box() : String {
    return if (getA<Int>() is A) "OK" else "FAIL"
}