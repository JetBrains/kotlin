class Test {
    override fun equals(other: Any?): Boolean {
        if (equals<caret>(other)) return true
        return false
    }
}