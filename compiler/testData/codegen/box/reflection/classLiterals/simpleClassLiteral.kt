// WITH_REFLECT
// WASM_STANDALONE
// ^^^ the test asserts on names of its own declarations; in a non-standalone run test classes are placed
//     in a sub-package, so those names would differ

package test

class A

fun box(): String {
    val klass = A::class
    return if (klass.toString() == "class test.A" ||
        // JS does not prepend with package name
        klass.toString() == "class A") "OK" else "Fail: $klass"
}
