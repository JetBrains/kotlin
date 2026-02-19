// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MODULE: dependency
// FILE: dependency.kt
package org.example

interface Base

abstract class Foo : Base

// MODULE: usage(dependency)
// FILE: usage.kt
package org.example

interface Base {
    fun bar()
}

abstract class Foo<caret>Impl : Foo() {

}
