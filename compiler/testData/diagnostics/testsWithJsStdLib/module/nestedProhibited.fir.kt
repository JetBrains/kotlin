@file:JsModule("foo")
package foo

@JsModule("A")
external class A {
    class Nested
}

@JsModule("B")
external object B

@JsModule("foo")
external fun foo(): Int

@JsModule("bar")
external val bar: Int

@JsNonModule
external val baz: Int
