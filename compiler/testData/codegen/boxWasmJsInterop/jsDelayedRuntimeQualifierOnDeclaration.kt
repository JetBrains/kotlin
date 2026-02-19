// During module instantiation, some JS references might be unresolved —
// for example, if a feature isn't supported in the current environment (like `Temporal` in KT-76509),
// or if an API is initialized later.
// So, qualifiers should generally be accessed at runtime.

// TARGET_BACKEND: WASM

// FILE: temporal.kt
package temporal

@JsQualifier("Temporal")
external class PlainTime // should be resolved at runtime

// FILE: main.kt
fun box(): String {
    return "OK"
}