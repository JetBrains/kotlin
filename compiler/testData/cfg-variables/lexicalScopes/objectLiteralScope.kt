fun foo() {
    "before"
    val bar = object {
        init {
            val x = 1
        }
        fun foo() {
            val a = 2
        }
    }
    "after"
}