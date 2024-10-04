// MODULE: lib
// MODULE_KIND: LibraryBinary
// FILE: lib.kt
package implicitJavaTypes

var listOfStrings = java.util.Arrays.asList("hello")

// MODULE: main(lib)
// FILE: main.kt
package test

import implicitJavaTypes.listOfStrings

fun usage() {
    list<caret>OfStrings
}
