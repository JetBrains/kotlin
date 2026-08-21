@file:OptIn(ExperimentalVersionOverloading::class)

tailrec fun versionedTailrec(
    n: Int = 10,
    @IntroducedAt("1") step: Int = 2,
    @IntroducedAt("2") adjust: Int = step - 1,
    @IntroducedAt("2") guard: Boolean = true,
): Int = when {
    !guard -> -1
    n <= 0 -> adjust
    else -> versionedTailrec(n - step, step, adjust, guard)
}

fun box(): String {
    if (versionedTailrec() != 1) return "FAIL default"
    if (versionedTailrec(5) != 1) return "FAIL first"
    if (versionedTailrec(5, 2) != 1) return "FAIL second"
    if (versionedTailrec(5, 2, 9) != 9) return "FAIL third"
    if (versionedTailrec(5, 2, 9, false) != -1) return "FAIL all"
    return "OK"
}
