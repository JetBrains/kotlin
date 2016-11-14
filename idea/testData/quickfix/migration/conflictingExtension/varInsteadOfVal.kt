// "Delete redundant extension property" "false"
// ACTION: Create test
// ACTION: Remove explicit type specification
import java.io.File

var File.<caret>name: String
    get() = getName()
    set(value) {}
