// MODULE: lib
// MODULE_KIND: Source
// FILE: Lib.kt
package one

class SimpleClass

// MODULE: dep(lib)
// MODULE_KIND: Source
// FILE: Dep.kt
package two

typealias SimpleTypeAlias = one.SimpleClass

// MODULE: main(dep, lib)
// FILE: usage.kt
package test

import two.SimpleTypeAlias

fun usage() {
    <caret>SimpleTypeAlias()
}