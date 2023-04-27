// FILE: main.kt
package test

import dependency.MyClass
import dependency.MyClass as MyAlias

/**
 * [MyAlias]
 */
fun usage() {}

// FILE: dependency.kt
package dependency

class MyClass
