// MODULE: dependency
// MODULE_KIND: Source
// FILE: Foo.kt
package lib

private interface FooProvider {
    fun foo() { println("foo") }
}

class FooScope : FooProvider

fun scope(fn: FooScope.() -> Unit) {
    FooScope().fn()
}

// MODULE: main(dependency)
// FILE: main.kt
import lib.scope

fun main() {
    scope {
        with(Any()) {
            with(Unit) {
                this@scope.<caret>foo()
            }
        }
    }
}

// callable: lib/FooProvider.foo