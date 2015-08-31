// "Delete redundant extension property" "false"

class C : Thread() {
    var Thread.<caret>priority: Int
        get() = getPriority()
        set(value) {
            this@C.setPriority(value)
        }
}
