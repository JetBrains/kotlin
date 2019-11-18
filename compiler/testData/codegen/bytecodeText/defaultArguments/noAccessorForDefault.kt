// FILE: Foo.kt

package foo

open class Foo() {
    protected fun foo(value: Boolean = false) = if (!value) "OK" else "fail5"
}

// FILE: Bar.kt

package bar

import foo.Foo

class Bar() : Foo() {
    fun execute(): String {
        return object { fun test() = foo() }.test ()
    }
}

fun box(): String {
    return Bar().execute()
}

// @bar/Bar.class:
// 0 access\$
// 0 INVOKESTATIC foo/Foo.foo\$default

// @bar/Bar$execute$1.class:
// 0 access\$
// 1 INVOKESTATIC foo/Foo.foo\$default