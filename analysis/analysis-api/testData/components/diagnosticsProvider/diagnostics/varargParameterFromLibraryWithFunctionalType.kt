// IGNORE_FE10

// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: Lib.kt
package pack

interface MyBuilder {
    val name: String
}

fun myDsl(vararg arguments: MyBuilder.() -> Unit) {

}

// MODULE: main(lib)
// FILE: usage.kt
package usage

import pack.myDsl

fun usage() {
    myDsl(
        {
            name
        },
        {
            name
        },
    )
}
