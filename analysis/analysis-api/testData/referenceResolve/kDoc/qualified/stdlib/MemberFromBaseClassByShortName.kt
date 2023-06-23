// FILE: main.kt

/**
 * [dependency.Child.from<caret>Base]
 */
fun usage() {}

// FILE: dependency.kt
package dependency

interface Base {
    fun fromBase()
}

interface Child : Base