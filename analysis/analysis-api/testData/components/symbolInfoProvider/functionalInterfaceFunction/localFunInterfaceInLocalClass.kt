fun usage() {
    class Local {
        fun interface IntPredicate {
            fun accept(i: Int): Boolean
        }
    }

    fun check(p: Local.<caret>IntPredicate) {}
}
