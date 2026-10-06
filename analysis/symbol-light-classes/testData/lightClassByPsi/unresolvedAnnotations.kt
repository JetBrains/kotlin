package test

@Unknown
class C @Unknown constructor(@Unknown val x: Int) {
    @Unknown
    fun simple(@Unknown p: String): @Unknown String = p

    @Unknown(1, "a")
    fun withArgs() {}

    @some.pkg.Unknown
    fun qualified() {}

    @field:Unknown
    @get:Unknown
    val prop: Int = 0

    fun typeArg(): List<@Unknown String> = emptyList()
}

// COMPILATION_ERRORS
