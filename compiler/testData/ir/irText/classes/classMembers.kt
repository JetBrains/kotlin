// FIR_IDENTICAL
// WITH_STDLIB
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS_IR_ES6

class C(x: Int, val y: Int, var z: Int = 1) {
    constructor() : this(0, 0, 0) {}

    val property: Int = 0

    val propertyWithGet: Int
        get() = 42

    var propertyWithGetAndSet: Int
        get() = z
        set(value) {
            z = value
        }

    fun function() {
        println("1")
    }

    fun Int.memberExtensionFunction() {
        println("2")
    }

    class NestedClass {
        fun function() {
            println("3")
        }

        fun Int.memberExtensionFunction() {
            println("4")
        }
    }

    interface NestedInterface {
        fun foo()
        fun bar() = foo()
    }

    companion object
}
