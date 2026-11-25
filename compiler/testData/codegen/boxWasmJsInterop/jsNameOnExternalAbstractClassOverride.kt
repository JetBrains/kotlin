// ISSUE: KT-88726
// An external class overrides functions of an `external abstract class` without @JsName: the override inherits the JS name of the base

// FILE: lib.js
globalThis.A = class A {};

globalThis.B = class B extends globalThis.A {
    foo_() { return "B.foo_" }
    foo() { return "B.foo" }
    bar() { return "B.bar" }
};

// FILE: main.kt
package foo

external abstract class A {
    @JsName("foo_")
    abstract fun foo(): String

    abstract fun bar(): String
}

external class B : A {
    override fun foo(): String

    override fun bar(): String
}

fun box(): String {
    val b = B()
    val a: A = b
    val r = "b.foo=${b.foo()};a.foo=${a.foo()};b.bar=${b.bar()};a.bar=${a.bar()}"
    return if (r == "b.foo=B.foo_;a.foo=B.foo_;b.bar=B.bar;a.bar=B.bar") "OK" else r
}
