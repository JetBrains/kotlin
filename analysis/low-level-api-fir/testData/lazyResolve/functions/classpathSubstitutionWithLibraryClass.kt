// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package org.example

interface Base

abstract class Foo : Base {
    fun o() {}
}

// MODULE: usage(lib)
// FILE: usage.kt
package org.example

interface Base {
    fun bar()
}

abstract class FooImpl : Foo() {
    override fun <caret>bar() {

    }
}
