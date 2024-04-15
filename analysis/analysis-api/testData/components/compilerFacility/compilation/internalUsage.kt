// MODULE: lib
// FILE: lib.kt

package lib

internal fun lib(): String = "foo"

// MODULE: main(lib)
// COMPILATION_ERRORS
// FILE: main.kt

import lib.lib

fun test() {
    <caret>lib()
}