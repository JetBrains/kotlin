// "Safe delete 'Imported'" "false"
// ACTION: Create test
// ACTION: Move 'ImportedClass' to separate file
// ACTION: Rename file to ImportedClass.kt
import ImportedClass as ClassAlias

class <caret>ImportedClass

fun use() {
    ClassAlias().hashCode()
}