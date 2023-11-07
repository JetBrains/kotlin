// TYPE: kotlin/Boolean
// TYPE: kotlin/String
// TYPE: kotlin/Int
// TYPE: dependency/Bar.Nested
// FILE: main.kt
package com.main

import dependency.Bar.Nested

fun foo() {}

// FILE: dependency.kt
package dependency

class Bar {
    class Nested
}