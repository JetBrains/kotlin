// "Delete redundant extension property" "true"
import java.io.File

class C {
    val File.<caret>name: String
        get() = getName()
}
