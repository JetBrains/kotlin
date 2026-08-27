class IntroducedOnly(
    @IntroducedAt("1") val a: Int = 1,
    @IntroducedAt("2") val b: String = "b",
)

class IntroducedAfterBase(
    val base: Long = 0L,
    @IntroducedAt("1") val a: Int = 1,
)

class WithMandatoryBase(
    val base: Long,
    @IntroducedAt("1") val a: Int = 1,
)

class Secondary(val base: Long) {
    constructor(
        @IntroducedAt("1") a: Int = 1,
    ) : this(a.toLong())
}

class Outer {
    inner class Inner(
        @IntroducedAt("1") val a: Int = 1,
    )
}
