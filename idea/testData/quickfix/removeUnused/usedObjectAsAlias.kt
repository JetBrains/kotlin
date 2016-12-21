// "Safe delete 'Imported'" "false"
// ACTION: Create test
// ACTION: Move 'Imported' to separate file
// ACTION: Rename file to Imported.kt
import Imported as Alias

object <caret>Imported

fun use() {
    Alias.hashCode()
}