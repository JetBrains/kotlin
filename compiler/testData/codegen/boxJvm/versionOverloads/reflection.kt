// TARGET_BACKEND: JVM
// WITH_REFLECT

@file:OptIn(ExperimentalVersionOverloading::class)

fun reflectedVersioned(
    a: Int = 1,
    @IntroducedAt("1") b: String = "b",
    @IntroducedAt("2") c: String = "c",
): String = "$a$b$c"

fun box(): String {
    val parameters = ::reflectedVersioned.parameters
    if (parameters.size != 3) return "FAIL parameter count"
    if (parameters.any { !it.isOptional }) return "FAIL optional parameters"
    if (::reflectedVersioned.callBy(mapOf(parameters[0] to 1, parameters[2] to "x")) != "1bx") {
        return "FAIL callBy gap"
    }
    if (::reflectedVersioned.callBy(emptyMap()) != "1bc") return "FAIL callBy defaults"
    return "OK"
}
