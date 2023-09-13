// MODULE: lib
// FILE: lib.kt

package lib

internal fun lib(): String = "foo"

// MODULE: main(lib)
// FILE: main.kt

import lib.lib

fun test() {
    lib()
}