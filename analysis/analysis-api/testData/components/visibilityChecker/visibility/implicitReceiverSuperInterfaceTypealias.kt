// MODULE: dependency
// MODULE_KIND: Source
// FILE: Foo.kt
package lib

internal interface FooProvider {
    fun foo() { println("foo") }
}

private typealias T = FooProvider

class FooScope : T

fun scope(fn: FooScope.() -> Unit) {
    FooScope().fn()
}

// MODULE: main(dependency)
// FILE: main.kt
import lib.scope

fun main() {
    scope {
        p<caret>rintln()
    }
}

// callable: lib/FooProvider.foo
