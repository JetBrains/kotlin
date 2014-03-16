fun foo() {
    "before"
    object A {
        {
            val a = 1
        }
        fun foo() {
            val b = 2
        }
    }
    "after"
}