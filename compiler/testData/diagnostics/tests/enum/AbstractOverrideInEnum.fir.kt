enum class E : T {
    ENTRY {
        override fun f() {
        }
    };

    abstract override fun f()
}

interface T {
    fun f()
}