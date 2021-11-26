// FILE: main.kt
package test

import dependency.Bar

fun usage(p: Bar) {}

// FILE: dependency.kt
package dependency

class Bar<T>