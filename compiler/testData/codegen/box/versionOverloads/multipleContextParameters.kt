// LANGUAGE: +ContextParameters

@file:OptIn(ExperimentalVersionOverloading::class)

context(contextPrefix: String, contextSuffix: Char)
fun String.contextualVersioned(
    value: String = contextPrefix,
    @IntroducedAt("1") suffix: String = contextSuffix.toString(),
): String = contextPrefix + this + value + suffix + contextSuffix

fun box(): String {
    with("P") {
        with('!') {
            if ("R".contextualVersioned() != "PRP!!") return "FAIL defaults"
            if ("R".contextualVersioned("V", "S") != "PRVS!") return "FAIL explicit"
        }
    }
    return "OK"
}
