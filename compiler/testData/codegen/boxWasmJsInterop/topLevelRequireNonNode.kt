// TARGET_BACKEND: WASM

@JsFun("() => typeof require")
external fun topLevelRequireType(): String

@JsFun("() => require === undefined")
external fun topLevelRequireIsUndefined(): Boolean

fun box(): String {
    val requireType = topLevelRequireType()
    if (requireType != "undefined") return "Unexpected require type: $requireType"
    if (!topLevelRequireIsUndefined()) return "Top-level require should be undefined"

    return "OK"
}
