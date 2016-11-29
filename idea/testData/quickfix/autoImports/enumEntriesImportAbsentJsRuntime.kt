// "Import" "false"
// ACTION: Convert property initializer to getter
// ACTION: Create object 'BLUE'
// ACTION: Create property 'BLUE'
// ACTION: Import member
// ACTION: Rename reference
// ERROR: Unresolved reference: BLUE

// Import should be present only in "Import member" action


package e

enum class ImportEnum {
    RED, GREEN, BLUE
}

class ImportClass {
    companion object {
        val BLUE = 0
    }
}

val v5 = B<caret>LUE