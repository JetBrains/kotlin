// LANGUAGE: +CompanionBlocks +CompanionExtensions
// WITH_STDLIB
@file:OptIn(ExperimentalVersionOverloading::class)

class GenericConstructor<T>(
    val value: T,
    @IntroducedAt("1") val fallback: T = value,
)

class GenericOwner<T>(private val defaultValue: T) {
    fun member(@IntroducedAt("1") fallback: T = defaultValue): T = fallback

    fun String.extension(@IntroducedAt("1") fallback: T = defaultValue): String = "$this/$fallback"
}

fun <K, V> genericPair(
    key: K,
    value: V,
    @IntroducedAt("1") pair: Pair<K, V> = Pair(key, value),
): String = "${pair.first}${pair.second}"

fun <T> genericLambda(
    value: T,
    @IntroducedAt("1") transform: (T) -> String = { it.toString() },
): String = transform(value)

inline fun <reified T> reifiedDefault(
    @IntroducedAt("1") value: T? = null,
): String = if (T::class == String::class) "String:$value" else "Other:$value"

class GenericOuter<T>(private val outerDefault: T) {
    inner class Inner(
        val value: T,
        @IntroducedAt("1") val fallback: T = outerDefault,
    )
}

class GenericCompanionOwner(val value: Any?, val fallback: Any?)

companion fun <T> GenericCompanionOwner.create(
    value: T,
    @IntroducedAt("1") fallback: T = value,
): GenericCompanionOwner = GenericCompanionOwner(value, fallback)

private var defaultEvaluationCount = 0

private fun nextDefaultValue(): Int {
    defaultEvaluationCount += 1
    return defaultEvaluationCount
}

fun sideEffectDefault(@IntroducedAt("1") value: Int = nextDefaultValue()): Int = value

fun box(): String {
    val genericConstructor = GenericConstructor("O")
    if (genericConstructor.value != "O" || genericConstructor.fallback != "O") return "fail1"

    val genericOwner = GenericOwner("default")
    if (genericOwner.member() != "default") return "fail2"
    if (with(genericOwner) { "receiver".extension() } != "receiver/default") return "fail3"

    if (genericPair("O", "K") != "OK") return "fail5"
    if (genericLambda("OK") != "OK") return "fail6"
    if (reifiedDefault<String>() != "String:null") return "fail7"

    val genericInner = GenericOuter("outer").Inner("inner")
    if (genericInner.value != "inner" || genericInner.fallback != "outer") return "fail8"

    val genericCompanion = GenericCompanionOwner.create("OK")
    if (genericCompanion.value != "OK" || genericCompanion.fallback != "OK") return "fail9"

    if (sideEffectDefault() != 1 || defaultEvaluationCount != 1) return "fail10"
    if (sideEffectDefault(42) != 42 || defaultEvaluationCount != 1) return "fail11"

    return "OK"
}
