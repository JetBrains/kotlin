// FILE: main.kt
package test

import dependency.Bar
import dependency.fooExt

fun usage(b: Bar) {
    b.fooExt()
}

// FILE: dependency.kt
package dependency

class Bar

fun Bar.fooExt(i: Int) {}