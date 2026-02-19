// MODULE: dependency
// MODULE_KIND: Source

// FILE: FooProvider.java
package lib;

interface FooProvider {
    default void foo() {
    }
}

// FILE: Foo.kt
package lib

class FooScope : FooProvider

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

// callable: lib/FooProvider.foo
