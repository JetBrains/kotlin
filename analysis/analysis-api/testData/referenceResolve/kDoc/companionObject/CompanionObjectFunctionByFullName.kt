// FILE: dependency.kt
package dependency

class WithCompanionObject {
    companion object {
        fun fromCompanion() {}
    }
}

// FILE: main.kt
/**
 * [dependency.WithCompanionObject.fromComp<caret>anion]
 */
fun usage() {}