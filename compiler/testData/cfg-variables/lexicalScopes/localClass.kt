fun foo() {
    "before"
    class A(val x: Int) {
        init {
            val a = x
        }
        fun foo() {
            val b = x
        }
    }
    "after"
}