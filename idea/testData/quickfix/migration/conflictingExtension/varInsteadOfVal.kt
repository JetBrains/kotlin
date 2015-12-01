// "Delete redundant extension property" "false"
// ACTION: Create test
import java.io.File

var File.<caret>name: String
    get() = getName()
    set(value) {}