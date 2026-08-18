// WORKS_WHEN_VALUE_CLASS
// WITH_STDLIB
@file:OptIn(ExperimentalVersionOverloading::class)
OPTIONAL_JVM_INLINE_ANNOTATION

value class VersionedValue(val value: Int) {
    constructor(
        text: String,
        @IntroducedAt("1") offset: Int = 0,
    ) : this(text.length + offset)

    fun render(@IntroducedAt("1") suffix: String = "!"): String = value.toString() + suffix
}

fun box(): String {
    if (VersionedValue("OK").value != 2) return "FAIL default"
    if (VersionedValue("OK", 1).value != 3) return "FAIL explicit"
    if (VersionedValue(2).render() != "2!") return "FAIL member default"
    if (VersionedValue(2).render("?") != "2?") return "FAIL member explicit"
    return "OK"
}
