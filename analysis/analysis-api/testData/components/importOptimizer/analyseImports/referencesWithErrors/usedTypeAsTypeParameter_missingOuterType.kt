// FILE: main.kt
package test

import dependency.Bar

//class WithGeneric<T>

fun usage(p: WithGeneric<Bar>) {}

// FILE: dependency.kt
package dependency

class Bar