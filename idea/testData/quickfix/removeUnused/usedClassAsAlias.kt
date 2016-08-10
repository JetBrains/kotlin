// "Safe delete 'Imported'" "false"
// ACTION: Create test
// ACTION: Move 'ImportedClass' to separate file
import ImportedClass as ClassAlias

class <caret>ImportedClass

fun use() {
    ClassAlias().hashCode()
}