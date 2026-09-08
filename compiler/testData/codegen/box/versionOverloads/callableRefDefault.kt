@file:OptIn(ExperimentalVersionOverloading::class)

fun callableReferenceTarget(value: String): Int = value.length

fun callableReferenceDefault(
    value: String = "a",
    @IntroducedAt("1") transform: (String) -> Int = ::callableReferenceTarget,
    @IntroducedAt("2") result: Int = transform(value),
): Int = result

fun box(): String {
    if (callableReferenceDefault() != 1) return "FAIL default"
    if (callableReferenceDefault("OK") != 2) return "FAIL first"

    val reference: (String, (String) -> Int, Int) -> Int = ::callableReferenceDefault
    if (reference("OK", ::callableReferenceTarget, 42) != 42) return "FAIL reference"

    return "OK"
}
