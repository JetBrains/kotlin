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

// JVM_IR uses the line number of the start of the `when` as the line number
// for the lookup/table switch. Therefore when the subject and the when is
// on separate lines the first step is on the subject, then steop to the when,
// then to the right branch.

// JVM_IR uses unoptimized lookup/table switches for all these cases. JVM
// does not. So on JVM there are direct jumps to the right branch for the
// last two whens.

// EXPECTATIONS JVM JVM_IR
// test.kt:27 box
// test.kt:5 stringSwitch
// test.kt:6 stringSwitch
// test.kt:5 stringSwitch
// test.kt:11 stringSwitch
// test.kt:12 stringSwitch
// test.kt:11 stringSwitch
// test.kt:18 stringSwitch
// EXPECTATIONS JVM_IR
// test.kt:17 stringSwitch
// EXPECTATIONS JVM JVM_IR
// test.kt:20 stringSwitch
// test.kt:17 stringSwitch
// test.kt:24 stringSwitch
// test.kt:28 box
// test.kt:5 stringSwitch
// test.kt:6 stringSwitch
// test.kt:7 stringSwitch
// test.kt:5 stringSwitch
// test.kt:11 stringSwitch
// EXPECTATIONS JVM_IR
// test.kt:12 stringSwitch
// EXPECTATIONS JVM JVM_IR
// test.kt:13 stringSwitch
// test.kt:11 stringSwitch
// test.kt:18 stringSwitch
// EXPECTATIONS JVM_IR
// test.kt:17 stringSwitch
// test.kt:20 stringSwitch
// EXPECTATIONS JVM JVM_IR
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
// EXPECTATIONS JVM_IR
// test.kt:12 stringSwitch
// test.kt:13 stringSwitch
// EXPECTATIONS JVM JVM_IR
// test.kt:14 stringSwitch
// test.kt:11 stringSwitch
// test.kt:18 stringSwitch
// EXPECTATIONS JVM_IR
// test.kt:17 stringSwitch
// test.kt:20 stringSwitch
// test.kt:21 stringSwitch
// EXPECTATIONS JVM JVM_IR
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
