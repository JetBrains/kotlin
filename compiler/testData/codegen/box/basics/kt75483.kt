// IGNORE_BACKEND_K2: WASM_JS, WASM_WASI

interface FeatureFlag<OptionType : Any> {
    val default: OptionType
}

abstract class BooleanFeatureFlag(
    override val default: Boolean,
) : FeatureFlag<Boolean>

fun <T : Any> currentValueOf(flag: FeatureFlag<T>): String {
    if (flag !is BooleanFeatureFlag) return "Fail 1"
    val default: Boolean = flag.default
    return if (default) "OK" else "Fail 2"
}

class BooleanFeatureFlagImpl: BooleanFeatureFlag(true)

fun box(): String {
    return currentValueOf<Boolean>(BooleanFeatureFlagImpl())
}
