fun usage() {
    class Local {
        fun interface IntPredicate {
            fun accept(i: Int): Boolean
        }
    }

    val isEven = Local.IntPredicate { it % 2 =<caret>= 0 }
}
