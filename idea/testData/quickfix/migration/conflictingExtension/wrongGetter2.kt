// "Delete redundant extension property" "false"
// ACTION: Convert property to function
import java.io.File

public val File.<caret>parent: File?
    get() = getParentFile()
