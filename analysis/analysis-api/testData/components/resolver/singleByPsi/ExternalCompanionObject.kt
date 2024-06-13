// FILE: main.kt
fun usage() {
    dependency.C.<caret>Companion
}

// FILE: C.kt
package dependency

class C {
    companion object {

    }
}

