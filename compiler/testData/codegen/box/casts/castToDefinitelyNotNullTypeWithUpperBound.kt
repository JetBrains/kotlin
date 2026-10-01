// IGNORE_BACKEND: WASM_JS, WASM_WASI
// WASM_MUTE_REASON: KT-88819
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: JS:2.4.20
// KT-49422: Fixed in 2.5.0-Beta2

var evaluations = 0

fun <T> produce(value: T): T {
    evaluations++
    return value
}

fun <T : CharSequence?> castWithUpperBound(value: T) = produce(value) as (T & Any)

fun box(): String {
    evaluations = 0
    if (castWithUpperBound<String?>("OK") != "OK") return "FAIL: value"
    if (evaluations != 1) return "FAIL: successful cast"

    evaluations = 0
    try {
        castWithUpperBound<String?>(null)
        return "FAIL: expected NPE"
    } catch (ex: NullPointerException) {
        if (evaluations != 1) return "FAIL: failed cast"
    }

    return "OK"
}
