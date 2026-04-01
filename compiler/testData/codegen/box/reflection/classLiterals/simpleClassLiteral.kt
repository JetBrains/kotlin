// WITH_REFLECT

package test

class A

fun box(): String {
    val klass = A::class
    return if (klass.toString() == "class test.A" ||
        // JS does not prepend with package name
        klass.toString() == "class A") "OK" else "Fail: $klass"
}
