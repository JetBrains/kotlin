// TARGET_BACKEND: JS_IR, JS_IR_ES6
// SPLIT_PER_CLASS
// ES_MODULES

// FILE: folder1/a.kt
package sample

enum class Status {
    START {
        override fun info() = "started"
    },
    DONE {
        override fun info() = "finished"
    };
    abstract fun info(): String
}

class Foo {
    class Nested {}

    fun memberMethod(x: Int): Int = x * 2

    fun mutateProp() {
        lazyProp = "mutated_by_Foo"
    }

    companion object {
        fun companionMethod(x: Int): Int = x + 10
    }
}
fun foo() {
    lazyProp = "mutated_by_foo_fun"
}
var lazyProp = "hello"
@OptIn(kotlin.ExperimentalStdlibApi::class)
@EagerInitialization
var eagerProp = "world"

// FILE: folder2/b.kt
package sample

@JsExport
class ExportedA {
    fun greet() = "Hello A"
}

@JsExport
class ExportedB {
    fun greet() = "Hello B"
}

class Bar {
    class Nested {}

    fun mutateProp2() {
        lazyProp2 = "mutated_by_Bar"
    }
}
fun bar() {}
var lazyProp2 = "hello"
@OptIn(kotlin.ExperimentalStdlibApi::class)
@EagerInitialization
var eagerProp2 = "world"

// FILE: main.kt
import sample.*

fun box(): String {
    val f = Foo()
    Foo.Nested()
    val b = Bar()
    Bar.Nested()

    if (Status.START.info() != "started") return "fail: Status.START"
    if (Status.DONE.info() != "finished") return "fail: Status.DONE"

    val ref1 = f::memberMethod
    if (ref1(5) != 10) return "fail: callable ref memberMethod"

    val ref2 = Foo.Companion::companionMethod
    if (ref2(5) != 15) return "fail: callable ref companionMethod"

    val expA = ExportedA()
    if (expA.greet() != "Hello A") return "fail: ExportedA"
    val expB = ExportedB()
    if (expB.greet() != "Hello B") return "fail: ExportedB"

    if (lazyProp != "hello") return "fail: lazyProp = $lazyProp"
    f.mutateProp()
    if (lazyProp != "mutated_by_Foo") return "fail: lazyProp after Foo.mutateProp = $lazyProp"
    foo()
    if (lazyProp != "mutated_by_foo_fun") return "fail: lazyProp after foo() = $lazyProp"

    if (eagerProp != "world") return "fail: eagerProp = $eagerProp"
    if (lazyProp2 != "hello") return "fail: lazyProp2 = $lazyProp2"
    b.mutateProp2()
    if (lazyProp2 != "mutated_by_Bar") return "fail: lazyProp2 after Bar.mutateProp2 = $lazyProp2"
    if (eagerProp2 != "world") return "fail: eagerProp2 = $eagerProp2"

    return "OK"
}
