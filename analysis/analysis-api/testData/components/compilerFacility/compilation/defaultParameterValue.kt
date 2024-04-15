// MODULE: lib
// FILE: lib.kt

package lib

fun lib(a: Int = 5): String {
    return "$a"
}

// MODULE: main(lib)
// COMPILATION_ERRORS
// FILE: main.kt

import lib.lib

fun test() {
    <caret>lib()
}