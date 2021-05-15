// FIR_COMPARISON
package a

import a.O.uniqueName

object O {
    fun uniqueName() {
    }
}

fun main() {
    uniqueN<caret>
}

// EXIST: { allLookupStrings: "uniqueName", itemText: "uniqueName", tailText: "()", typeText: "Unit", attributes: "" }
// NUMBER: 1