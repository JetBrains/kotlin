
// Top level functions.
fun hello() {
    println("Hello, dynamic!")
}

fun getString() = "Kotlin/Native"

// Class with inheritance.
open class Base {
    open fun foo() = println("Base.foo")

    open fun fooParam(arg0: String, arg1: Int) = println("Base.fooParam: $arg0 $arg1")

}

class Child : Base() {
    override fun fooParam(arg0: String, arg1: Int) = println("Child.fooParam: $arg0 $arg1")

    val roProperty: Int
        get() = 42

    var rwProperty: Int = 0
        get() = field
        set(value) { field = value + 1 }
}

// Interface.
interface I {
    fun foo(arg0: String, arg1: Int, arg2: I)
    fun fooImpl() = foo("Hi", 239, this)
}

open class Impl1: I {
    override fun foo(arg0: String, arg1: Int, arg2: I) {
        println("Impl1.I: $arg0 $arg1 ${arg2::class.qualifiedName}")
    }
}

class Impl2 : Impl1() {
    override fun foo(arg0: String, arg1: Int, arg2: I) {
        println("Impl2.I: $arg0 $arg1 ${arg2::class.qualifiedName}")
    }
}