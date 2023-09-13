// MODULE: lib
// FILE: lib.kt

package lib

fun lib(a: Int = 5): String {
    return "$a"
}

// MODULE: main(lib)
// FILE: main.kt

import lib.lib

fun test() {
    lib()
}