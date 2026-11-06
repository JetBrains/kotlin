// TARGET_BACKEND: JS_IR, JS_IR_ES6
// SPLIT_PER_CLASS
// ES_MODULES

// FILE: folder1/a.kt
package sample

class Foo {
    class Nested {}
}
fun foo() {}
var lazyProp = "hello"
@OptIn(kotlin.ExperimentalStdlibApi::class)
@EagerInitialization
var eagerProp = "world"

// FILE: folder2/b.kt
package sample

class Bar {
    class Nested {}
}
fun bar() {}
var lazyProp2 = "hello"
@OptIn(kotlin.ExperimentalStdlibApi::class)
@EagerInitialization
var eagerProp2 = "world"

// FILE: main.kt
import sample.*

fun box(): String {
    Foo()
    Foo.Nested()
    Bar()
    Bar.Nested()
    foo()
    bar()
    if (lazyProp != "hello") return "fail: lazyProp = $lazyProp"
    if (eagerProp != "world") return "fail: eagerProp = $eagerProp"
    if (lazyProp2 != "hello") return "fail: lazyProp2 = $lazyProp2"
    if (eagerProp2 != "world") return "fail: eagerProp2 = $eagerProp2"
    return "OK"
}
