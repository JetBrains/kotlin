// MODULE: dependency
// MODULE_KIND: Source
// FILE: Foo.kt
package lib

private interface Outer {
    interface FooProvider {
        fun foo() { println("foo") }
    }
}

class FooScope : Outer.FooProvider

fun scope(fn: FooScope.() -> Unit) {
    FooScope().fn()
}

// MODULE: main(dependency)
// FILE: main.kt
import lib.scope

fun main() {
    scope {
        this.<caret>foo()
    }
}

// callable: lib/Outer.FooProvider.foo
