@file:OptIn(ExperimentalVersionOverloading::class)

private var defaultEvaluationCount = 0

private fun nextDefault(label: String): String {
    defaultEvaluationCount += 1
    return "$label$defaultEvaluationCount"
}

fun multipleSideEffects(
    value: String = "A",
    @IntroducedAt("1") first: String = nextDefault("B"),
    @IntroducedAt("2") second: String = nextDefault("C"),
): String = "$value/$first/$second"

fun throwingVersionedDefault(
    @IntroducedAt("1") value: String = throw IllegalStateException("default"),
): String = value

fun dependentDefaults(
    a: Int,
    @IntroducedAt("1") b: Int = a,
    @IntroducedAt("2") c: Int = b,
): Int = a + b + c

fun sameVersion(
    a: Int,
    @IntroducedAt("1") b: Int = 1,
    @IntroducedAt("1") c: Int = 2,
): Int = a + b + c

fun mixedDefaults(
    a: Int,
    @IntroducedAt("1") b: Int = 1,
    c: Int = 2,
    @IntroducedAt("2") d: Int = 3,
): Int = a + b + c + d

fun box(): String {
    defaultEvaluationCount = 0
    if (multipleSideEffects() != "A/B1/C2" || defaultEvaluationCount != 2) return "FAIL default"

    defaultEvaluationCount = 0
    if (multipleSideEffects("X") != "X/B1/C2" || defaultEvaluationCount != 2) return "FAIL partial"

    defaultEvaluationCount = 0
    if (multipleSideEffects("X", "Y") != "X/Y/C1" || defaultEvaluationCount != 1) return "FAIL second partial"

    defaultEvaluationCount = 0
    if (multipleSideEffects("X", "Y", "Z") != "X/Y/Z" || defaultEvaluationCount != 0) return "FAIL full"

    try {
        throwingVersionedDefault()
        return "FAIL no exception"
    } catch (_: IllegalStateException) {
    }

    if (dependentDefaults(1) != 3) return "FAIL dependent defaults"
    if (sameVersion(1) != 4) return "FAIL same version"
    if (mixedDefaults(1) != 7) return "FAIL mixed defaults"

    return "OK"
}
