// IGNORE_BACKEND: NATIVE
// WITH_REFLECT
package test

class A

fun box(): String {
    val klass = A::class
    return if (klass.toString() == "class test.A") "OK" else "Fail: $klass"
}
