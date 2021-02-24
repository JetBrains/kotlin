// EXPECTED_REACHABLE_NODES: 1239
// DONT_TARGET_EXACT_BACKEND: WASM
// PROPERTY_LAZY_INITIALIZATION

// FILE: A.kt

val a = "A".let {
    flag = !flag
    if (flag) {
        it
    } else {
        "!A"
    }
}

val b = "B".let {
    flag = !flag
    if (!flag) {
        it
    } else {
        "!B"
    }
}

// FILE: B.kt
var flag: Boolean = false

// FILE: main.kt

fun box(): String {
    return if (
        a == "A" && b == "B"
    )
        "OK"
    else "a = $a; b = ${b}"
}