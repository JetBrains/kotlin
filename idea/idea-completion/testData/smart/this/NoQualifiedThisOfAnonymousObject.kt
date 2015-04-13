open class Foo

fun foo(f: Foo){}

fun bar() {
    foo(object: Foo() {
        inner class Inner {
            fun f() {
                val x: Foo = <caret>
            }
        }
    })
}

// ABSENT: this@foo
