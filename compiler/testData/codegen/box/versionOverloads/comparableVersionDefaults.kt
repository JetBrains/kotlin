@file:OptIn(ExperimentalVersionOverloading::class)

fun comparableVersionDefaults(
    base: Int = 1,
    @IntroducedAt("1.9") minor: Int = 19,
    @IntroducedAt("1.10-beta2") beta: Int = minor + 1,
    @IntroducedAt("1.10") stable: Int = beta + 1,
    @IntroducedAt("2") major: Int = stable + 1,
): String = "$base/$minor/$beta/$stable/$major"

fun box(): String {
    if (comparableVersionDefaults() != "1/19/20/21/22") return "FAIL default"
    if (comparableVersionDefaults(0) != "0/19/20/21/22") return "FAIL base"
    if (comparableVersionDefaults(0, 100) != "0/100/101/102/103") return "FAIL minor"
    if (comparableVersionDefaults(0, 100, 200) != "0/100/200/201/202") return "FAIL beta"
    return "OK"
}
