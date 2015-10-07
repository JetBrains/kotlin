package a

import a.O.uniqueName

object O {
    fun uniqueName() {
    }
}

fun main() {
    uniqueN<caret>
}

// EXIST: uniqueName
// NUMBER: 1