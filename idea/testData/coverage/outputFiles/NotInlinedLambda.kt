package org.demo.coverage

public class Foo {
    public fun forEach(fn: (Any?) -> Unit): Unit {
    }

    public fun bar() {
        forEach {
            println("foo")
        }
    }
}
