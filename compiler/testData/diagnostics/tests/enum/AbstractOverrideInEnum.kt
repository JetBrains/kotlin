enum class E : T {
    ENTRY {
        override fun f() {
        }
    }

    abstract override fun f()
}

trait T {
    fun f()
}