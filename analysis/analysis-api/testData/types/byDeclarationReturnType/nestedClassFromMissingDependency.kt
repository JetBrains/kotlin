// COMPILATION_ERRORS

// MODULE: dep
// FILE: dep.kt
package dep

class Outer {
    class Nested
}

// MODULE: lib(dep)
// FILE: lib.kt
package lib

import dep.Outer

fun nested(): Outer.Nested = TODO()

// MODULE: main(lib)
// FILE: main.kt
fun fo<caret>o() = lib.nested()
