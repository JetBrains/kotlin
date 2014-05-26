dclass A {
    fun foo() {
        inlineFun { "test" }
    }

    inline fun inlineFun(lambda: () -> Unit) {
        val s = object {
            fun run() {
                lambda()
            }
        }
        s.run()
    }
}
