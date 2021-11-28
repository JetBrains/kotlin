// FILE: main.kt
package test

import dependency.Bar
import dependency.Bar as BarAlias

fun usage(p: Bar) {}

// FILE: dependency.kt
package dependency

class Bar