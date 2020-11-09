// TARGET_BACKEND: JS_IR

// FILE: A.kt

val a = "A"

// FILE: B.kt
val b = "B".apply {}

val c = "C"

// FILE: main.kt

fun box(): String {
    return if (js("a") == "A" && js("typeof b") == "undefined" && js("typeof c") == "undefined")
        "OK"
    else "fail"
}