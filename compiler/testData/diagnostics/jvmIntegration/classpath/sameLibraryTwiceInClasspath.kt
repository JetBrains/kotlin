// FIR_IDENTICAL
// MODULE: library1
// FILE: source.kt
package testing

object TopLevelObject

class Outer {
    inner class Inner
    class Nested
}

// MODULE: library2
// FILE: source.kt
package testing

object TopLevelObject

class Outer {
    inner class Inner
    class Nested
}

// MODULE: main(library1, library2)
// FILE: source.kt
package test

import testing.*

val testObjectProperty = TopLevelObject

val outer = Outer()
val inn3r = Outer().Inner()
val nested = Outer.Nested()
