// MODULE: lib1
// MODULE_KIND: LibraryBinary
// FILE: lib1.kt
package lib
val String.foo: Int get() = 0
val Int.foo: Int get() = 1

// MODULE: lib2
// MODULE_KIND: LibraryBinary
// FILE: lib2.kt
package lib
val String.foo: Int get() = 0
val Char.foo: Int get() = 1

// MODULE: lib3
// MODULE_KIND: LibraryBinary
// FILE: lib3.kt
package lib
val Int.foo: Int get() = 0
val Char.foo: Int get() = 1

// MODULE: context(lib1, lib2, lib3)
// FILE: context.kt
import lib.*

fun test() {
    <caret_context>val x = 0
}

// MODULE: main
// MODULE_KIND: CodeFragment
// CONTEXT_MODULE: context
"x".foo + 1.foo + 'c'.foo