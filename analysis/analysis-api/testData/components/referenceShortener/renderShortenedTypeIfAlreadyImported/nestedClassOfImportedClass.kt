// TYPE: kotlin/Boolean
// TYPE: kotlin/String
// TYPE: kotlin/Int
// TYPE: dependency/Bar.Nested
// TYPE: dependency/X.Y.Z.W
// FILE: main.kt
package com.main

import dependency.Bar
import dependency.X.Y

fun foo() {}

// FILE: dependency.kt
package dependency

class Bar {
    class Nested
}

class X {
    class Y {
        class Z {
            class W
        }
    }
}