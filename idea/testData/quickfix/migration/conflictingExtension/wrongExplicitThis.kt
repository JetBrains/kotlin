// "Delete redundant extension property" "false"
// ACTION: Convert property to function
// ACTION: Move to companion object
// ACTION: Remove explicit type specification

class C : Thread() {
    val Thread.<caret>priority: Int
        get() = this@C.getPriority()
}
