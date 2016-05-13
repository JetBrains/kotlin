// "Safe delete 'Imported'" "false"
// ACTION: Create test
// ACTION: Move 'Imported' to separate file
import Imported as Alias

object <caret>Imported

fun use() {
    Alias.hashCode()
}