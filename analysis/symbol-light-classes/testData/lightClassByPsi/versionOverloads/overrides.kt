open class Base {
    open fun overridden(
        a: String = "a",
        b: String = "b",
    ) = a + b
}

class FinalOverride : Base() {
    override fun overridden(
        a: String,
        @IntroducedAt("1") b: String,
    ) = a + b
}

// LIGHT_ELEMENTS_NO_DECLARATION: FinalOverride.class[overridden]
