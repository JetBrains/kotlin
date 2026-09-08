@file:OptIn(ExperimentalVersionOverloading::class)

@PublishedApi
internal fun invokeTransform(value: Int, transform: (Int) -> String): String = transform(value)

inline fun crossinlineTrailing(
    @IntroducedAt("1") value: Int = 1,
    crossinline transform: (Int) -> String,
): String = invokeTransform(value) { transform(it) }

inline fun noinlineTrailing(
    @IntroducedAt("1") value: Int = 2,
    noinline transform: (Int) -> String,
): String = transform(value)

fun box(): String {
    if (crossinlineTrailing { "C$it" } != "C1") return "FAIL crossinline default"
    if (crossinlineTrailing(3) { "C$it" } != "C3") return "FAIL crossinline explicit"
    if (noinlineTrailing { "N$it" } != "N2") return "FAIL noinline default"
    if (noinlineTrailing(4) { "N$it" } != "N4") return "FAIL noinline explicit"
    return "OK"
}
