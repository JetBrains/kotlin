// MODULE: dependency
// MODULE_KIND: Source
// FILE: Foo.kt
package lib

abstract class A { // can't be private/internal
    abstract fun foo()
}

abstract class FooScope : A()

fun scope(fn: FooScope.() -> Unit) {
}

// MODULE: main(dependency)
// FILE: main.kt
import lib.scope

fun main() {
    scope {
        this.<caret>foo()
    }
}

// callable: lib/A.foo
