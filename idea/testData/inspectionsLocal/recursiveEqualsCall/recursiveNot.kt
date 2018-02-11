class Test {
    override fun equals(other: Any?): Boolean {
        if (<caret>this != other) return false
        return true
    }
}