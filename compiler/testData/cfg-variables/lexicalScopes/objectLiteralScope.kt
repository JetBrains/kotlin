fun foo() {
    "before"
    val bar = object {
        {
            val x = 1
        }
        fun foo() {
            val a = 2
        }
    }
    "after"
}