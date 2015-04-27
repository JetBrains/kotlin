fun foo() {
    "before"
    object A {
        init {
            val a = 1
        }
        fun foo() {
            val b = 2
        }
    }
    "after"
}