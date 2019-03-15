package test

interface Base {
    companion object {
        @Suppress("INAPPLICABLE_JVM_FIELD")
        @JvmField
        val foo = object : Base {}
    }
}
