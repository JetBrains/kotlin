@file:OptIn(ExperimentalVersionOverloading::class)

open class VersionedBase(
    val first: Int = 1,
    @IntroducedAt("1") val second: String = "A",
    @IntroducedAt("2") val third: Int = 3,
)

class VersionedDerived : VersionedBase {
    constructor(first: Int, @IntroducedAt("1") second: String = "B") : super(first, second)
    constructor(@IntroducedAt("1") second: String = "C") : super(2, second)
    constructor(flag: Boolean) : super(3)
}

class VersionedDelegating(
    first: Int,
    @IntroducedAt("1") second: String = "D",
) : VersionedBase(first, second) {
    constructor(@IntroducedAt("1") second: String = "E") : this(2, second)
    constructor(flag: Boolean) : this(3)
}

fun box(): String {
    val base = VersionedBase()
    if (base.first != 1 || base.second != "A" || base.third != 3) return "FAIL base"

    val derived1 = VersionedDerived()
    val derived2 = VersionedDerived(2)
    val derived3 = VersionedDerived(2, "K")
    val derived4 = VersionedDerived("K")
    val derived5 = VersionedDerived(true)
    if (derived1.second != "C") return "FAIL derived1"
    if (derived2.second != "B") return "FAIL derived2"
    if (derived3.second != "K") return "FAIL derived3"
    if (derived4.second != "K") return "FAIL derived4"
    if (derived5.second != "A") return "FAIL derived5"

    val delegating1 = VersionedDelegating()
    val delegating2 = VersionedDelegating(2)
    val delegating3 = VersionedDelegating(2, "K")
    val delegating4 = VersionedDelegating("K")
    val delegating5 = VersionedDelegating(true)
    if (delegating1.second != "E") return "FAIL delegating1"
    if (delegating2.second != "D") return "FAIL delegating2"
    if (delegating3.second != "K") return "FAIL delegating3"
    if (delegating4.second != "K") return "FAIL delegating4"
    if (delegating5.second != "D") return "FAIL delegating5"

    return "OK"
}
