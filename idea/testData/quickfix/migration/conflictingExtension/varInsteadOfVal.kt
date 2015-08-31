// "Delete redundant extension property" "false"
import java.io.File

var File.<caret>name: String
    get() = getName()
    set(value) {}