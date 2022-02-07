enum class SomeEnum(val x: String) {
    Some("hello") {
        override fun foo() {
            println()
        }
    },
    Other("world") {
        override fun foo() {}
    };

    abstract fun foo()
}
