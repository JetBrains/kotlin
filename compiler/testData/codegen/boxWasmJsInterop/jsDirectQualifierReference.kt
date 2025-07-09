// TARGET_BACKEND: WASM

// FILE: temporal.kt
@file:JsQualifier("Temporal")
package temporal

external class PlainTime

// FILE: main.kt
fun box(): String {
    return "OK"
}