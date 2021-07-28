fun foo() {
    val base = object {
        fun bar() = object {
            fun buz() = foobar
        }
        val foobar = ""
    }
}