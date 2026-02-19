// MODULE: dependency
// MODULE_KIND: Source
// FILE: Foo.kt
package lib

internal interface FooProvider {
    fun foo() { println("foo") }
}

private interface Middle : FooProvider

class FooScope : Middle

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
