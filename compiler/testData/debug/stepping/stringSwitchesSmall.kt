// IGNORE_BACKEND: WASM
// FILE: test.kt

fun stringSwitch(x: String) {
    val l = when {
        x == "x" -> 1
        x == "xy" -> 2
        else -> -1
    }

    val l2 = when (x) {
        "x" -> 1
        "xy" -> 2
        else -> -1
    }

    val l3 = when
        (x)
    {
        "x" -> 1
        "xy" -> 2
        else -> -1
    }
}

fun box() {
    stringSwitch("x")
    stringSwitch("xy")
    stringSwitch("nope")
}

// EXPECTATIONS JVM_IR
// test.kt:27 box
// test.kt:5 stringSwitch
// test.kt:6 stringSwitch
// test.kt:5 stringSwitch
// test.kt:11 stringSwitch
// test.kt:12 stringSwitch
// test.kt:11 stringSwitch
// test.kt:18 stringSwitch
// test.kt:17 stringSwitch
// test.kt:20 stringSwitch
// test.kt:17 stringSwitch
// test.kt:24 stringSwitch
// test.kt:28 box
// test.kt:5 stringSwitch
// test.kt:6 stringSwitch
// test.kt:7 stringSwitch
// test.kt:5 stringSwitch
// test.kt:11 stringSwitch
// test.kt:12 stringSwitch
// test.kt:13 stringSwitch
// test.kt:11 stringSwitch
// test.kt:18 stringSwitch
// test.kt:17 stringSwitch
// test.kt:20 stringSwitch
// test.kt:21 stringSwitch
// test.kt:17 stringSwitch
// test.kt:24 stringSwitch
// test.kt:29 box
// test.kt:5 stringSwitch
// test.kt:6 stringSwitch
// test.kt:7 stringSwitch
// test.kt:8 stringSwitch
// test.kt:5 stringSwitch
// test.kt:11 stringSwitch
// test.kt:12 stringSwitch
// test.kt:13 stringSwitch
// test.kt:14 stringSwitch
// test.kt:11 stringSwitch
// test.kt:18 stringSwitch
// test.kt:17 stringSwitch
// test.kt:20 stringSwitch
// test.kt:21 stringSwitch
// test.kt:22 stringSwitch
// test.kt:17 stringSwitch
// test.kt:24 stringSwitch
// test.kt:30 box

// EXPECTATIONS JS_IR
// test.kt:27 box
// test.kt:5 stringSwitch
// test.kt:6 stringSwitch
// test.kt:11 stringSwitch
// test.kt:12 stringSwitch
// test.kt:17 stringSwitch
// test.kt:20 stringSwitch
// test.kt:24 stringSwitch
// test.kt:28 box
// test.kt:5 stringSwitch
// test.kt:7 stringSwitch
// test.kt:11 stringSwitch
// test.kt:13 stringSwitch
// test.kt:17 stringSwitch
// test.kt:21 stringSwitch
// test.kt:24 stringSwitch
// test.kt:29 box
// test.kt:5 stringSwitch
// test.kt:8 stringSwitch
// test.kt:11 stringSwitch
// test.kt:14 stringSwitch
// test.kt:17 stringSwitch
// test.kt:22 stringSwitch
// test.kt:24 stringSwitch
// test.kt:30 box
