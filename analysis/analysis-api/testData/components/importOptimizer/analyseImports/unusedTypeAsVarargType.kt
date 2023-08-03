// FILE: main.kt
package test

import dependency.Bar

fun usage(vararg p: dependency.Bar) {}

// FILE: dependency.kt
package dependency

class Bar