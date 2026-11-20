// JVM_DEFAULT_MODE: disable

interface OnlyPrivateImplementations {
    private fun foo() {}

    private val bar: Int
        get() = 0

    fun baz()
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: OnlyPrivateImplementations.class[bar;foo]
