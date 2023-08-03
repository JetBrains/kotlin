// FILE: main.kt
package test

import dependency.Bar

fun usage(vararg p: Bar) {}

// FILE: dependency.kt
package dependency

class Bar