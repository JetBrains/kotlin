@file:OptIn(ExperimentalVersionOverloading::class)

class VersionedOperators(private val value: Int) {
    operator fun invoke(@IntroducedAt("1") delta: Int = 0): Int = value + delta

    operator fun plus(other: VersionedOperators): VersionedOperators = VersionedOperators(value + other.value)

    fun plus(@IntroducedAt("1") other: Int = 1): VersionedOperators = VersionedOperators(value + other)

    operator fun contains(item: Int): Boolean = value == item

    fun contains(@IntroducedAt("1") item: String = value.toString()): Boolean = value.toString() == item

    operator fun compareTo(other: Int): Int = value.compareTo(other)

    fun compareTo(@IntroducedAt("1") other: String = value.toString()): Int = if (other == value.toString()) 0 else -1

    fun getValue(): Int = value
}

fun box(): String {
    val value = VersionedOperators(2)
    if (value() != 2) return "FAIL invoke default"
    if (value(3) != 5) return "FAIL invoke explicit"
    if (value.plus().getValue() != 3) return "FAIL plus default"
    if ((value + VersionedOperators(3)).getValue() != 5) return "FAIL plus operator"
    if (!value.contains()) return "FAIL contains default"
    if (2 !in value) return "FAIL contains operator"
    if (value.compareTo() != 0) return "FAIL compareTo default"
    if (!(value < 3)) return "FAIL compareTo operator"
    return "OK"
}
