// FILE: main.kt
package test

import dependency.Bar as BarAlias

fun usage(p: BarAlias) {}

// FILE: dependency.kt
package dependency

class Bar