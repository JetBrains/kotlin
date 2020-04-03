// IGNORE_BACKEND: JS
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: NATIVE
// MODULE: lib
// FILE: lib.kt

open class Base<T> {
    open fun foo(p1: T): String { return "p1:$p1" }
    open fun foo(p2: String): String { return "p2:$p2" }
}
class Derived : Base<String>()



// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    val d = Derived()
    if (d.foo(p1 = "42") != "p1:42") return "FAIL1"
    if (d.foo(p2 = "24") != "p2:24") return "FAIL2"

    return "OK"
}