// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package implicitJavaTypes

var listOfStrings = java.util.Arrays.asList("hello")

var string = listOfStrings.get(0)

// MODULE: main(lib)
// FILE: main.kt
package test

import implicitJavaTypes.string

fun usage() {
    st<caret>ring
}
