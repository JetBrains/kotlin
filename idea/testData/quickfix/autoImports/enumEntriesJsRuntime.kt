// "Import" "true"
package e

enum class ImportEnum {
    RED, GREEN, BLUE
}

class ImportClass {
    companion object {
        val BLUE = 0
    }
}

val v5 = <caret>BLUE