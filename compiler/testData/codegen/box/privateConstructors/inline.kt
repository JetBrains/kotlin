// See also KT-6299
// FILE: lib.kt
public open class Outer private constructor() {
    companion object {
        internal inline fun foo() = Outer()
    }
}

// FILE: main.kt
fun box(): String {
    val outer = Outer.foo()
    return "OK"
}