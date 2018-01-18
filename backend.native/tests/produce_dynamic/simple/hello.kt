import kotlinx.cinterop.*

// Top level functions.
fun hello() {
    println("Hello, dynamic!")
}

fun getString() = "Kotlin/Native"

// Class with inheritance.
open class Base {
    open fun foo() = println("Base.foo")

    open fun fooParam(arg0: String, arg1: Int) = println("Base.fooParam: $arg0 $arg1")

    @konan.internal.CName(fullName = "", shortName = "strangeName") fun странноеИмя() = 111

}

// Top level functions.
@konan.internal.CName(fullName = "topLevelFunctionFromC", shortName = "topLevelFunctionFromCShort")
fun topLevelFunction(x1: Int, x2: Int) = x1 - x2

@konan.internal.CName("topLevelFunctionVoidFromC")
fun topLevelFunctionVoid(x1: Int, pointer: COpaquePointer?) {
    assert(x1 == 42)
    assert(pointer == null)
}

// Enum.
enum class Enum(val code: Int) {
    ONE(1),
    TWO(2),
    HUNDRED(100)
}

// Object.
interface Codeable {
    fun asCode(): Int
}

val an_object = object : Codeable {
    override fun asCode() = 42
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