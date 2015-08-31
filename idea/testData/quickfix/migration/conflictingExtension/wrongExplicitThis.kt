// "Delete redundant extension property" "false"
// ACTION: Convert property to function

class C : Thread() {
    val Thread.<caret>priority: Int
        get() = this@C.getPriority()
}
