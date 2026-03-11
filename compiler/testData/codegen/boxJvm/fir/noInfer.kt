// WITH_STDLIB
// TARGET_BACKEND: JVM_IR
// ISSUE: KT-62578

// MODULE: lib
// FILE: lib.kt
interface Base
class Derived : Base

data class Box<out T>(val className: String)

val emptyBox: Box<Nothing>
    get() = Box<Nothing>("Nothing")

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
inline fun <reified T> foo(x: @kotlin.internal.NoInfer T): Box<T>? {
    val klass = T::class
    return Box(klass.simpleName ?: "<unknown>")
}

fun test_1(): Box<Base> {
    return foo(Derived()) ?: emptyBox
}

// MODULE: main(lib)
// FILE: main.kt

fun test_2(): Box<Base> {
    return foo(Derived()) ?: emptyBox
}

fun box(): String {
    val expectedName = "Base"
    val box1 = test_1()
    if (box1.className != expectedName) return "Fail 1: $box1"
    val box2 = test_2()
    if (box2.className != expectedName) return "Fail 2: $box2"
    return "OK"
}
