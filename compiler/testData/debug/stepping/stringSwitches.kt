// IGNORE_BACKEND: WASM
// FILE: test.kt

fun stringSwitch(x: String) {
    val l = when {
        x == "x" -> 1
        x == "xy" -> 2
        x == "xyz" -> 3
        else -> -1
    }

    val l2 = when (x) {
        "x" -> 1
        "xy" -> 2
        "xyz" -> 3
        else -> -1
    }

    val l3 = when
        (x)
    {
        "x" -> 1
        "xy" -> 2
        "xyz" -> 3
        else -> -1
    }
}

fun box() {
    stringSwitch("x")
    stringSwitch("xy")
    stringSwitch("xyz")
    stringSwitch("nope")
}

// JVM_IR uses the line number of the start of the `when` as the line number
// for the lookup/table switch. Therefore when the subject and the when is
// on separate lines the first step is on the subject, then steop to the when,
// then to the right branch.

// JVM_IR uses optimized lookup/table switches for all these cases. JVM
// does not. So on JVM there are steps on each condition evaluation for
// the first `when`.

// EXPECTATIONS JVM JVM_IR
// test.kt:30 box
// test.kt:5 stringSwitch
// test.kt:6 stringSwitch
// test.kt:5 stringSwitch
// test.kt:12 stringSwitch
// test.kt:13 stringSwitch
// test.kt:12 stringSwitch
// test.kt:20 stringSwitch
// EXPECTATIONS JVM_IR
// test.kt:19 stringSwitch
// EXPECTATIONS JVM JVM_IR
// test.kt:22 stringSwitch
// test.kt:19 stringSwitch
// test.kt:27 stringSwitch
// test.kt:31 box
// test.kt:5 stringSwitch
// EXPECTATIONS JVM
// test.kt:6 stringSwitch
// EXPECTATIONS JVM JVM_IR
// test.kt:7 stringSwitch
// test.kt:5 stringSwitch
// test.kt:12 stringSwitch
// test.kt:14 stringSwitch
// test.kt:12 stringSwitch
// test.kt:20 stringSwitch
// EXPECTATIONS JVM_IR
// test.kt:19 stringSwitch
// EXPECTATIONS JVM JVM_IR
// test.kt:23 stringSwitch
// test.kt:19 stringSwitch
// test.kt:27 stringSwitch
// test.kt:32 box
// test.kt:5 stringSwitch
// EXPECTATIONS JVM
// test.kt:6 stringSwitch
// test.kt:7 stringSwitch
// EXPECTATIONS JVM JVM_IR
// test.kt:8 stringSwitch
// test.kt:5 stringSwitch
// test.kt:12 stringSwitch
// test.kt:15 stringSwitch
// test.kt:12 stringSwitch
// test.kt:20 stringSwitch
// EXPECTATIONS JVM_IR
// test.kt:19 stringSwitch
// EXPECTATIONS JVM JVM_IR
// test.kt:24 stringSwitch
// test.kt:19 stringSwitch
// test.kt:27 stringSwitch
// test.kt:33 box
// test.kt:5 stringSwitch
// EXPECTATIONS JVM
// test.kt:6 stringSwitch
// test.kt:7 stringSwitch
// test.kt:8 stringSwitch
// EXPECTATIONS JVM JVM_IR
// test.kt:9 stringSwitch
// test.kt:5 stringSwitch
// test.kt:12 stringSwitch
// test.kt:16 stringSwitch
// test.kt:12 stringSwitch
// test.kt:20 stringSwitch
// EXPECTATIONS JVM_IR
// test.kt:19 stringSwitch
// EXPECTATIONS JVM JVM_IR
// test.kt:25 stringSwitch
// test.kt:19 stringSwitch
// test.kt:27 stringSwitch
// test.kt:34 box

// EXPECTATIONS JS_IR
// test.kt:30 box
// test.kt:5 stringSwitch
// test.kt:6 stringSwitch
// test.kt:12 stringSwitch
// test.kt:13 stringSwitch
// test.kt:19 stringSwitch
// test.kt:22 stringSwitch
// test.kt:27 stringSwitch
// test.kt:31 box
// test.kt:5 stringSwitch
// test.kt:7 stringSwitch
// test.kt:12 stringSwitch
// test.kt:14 stringSwitch
// test.kt:19 stringSwitch
// test.kt:23 stringSwitch
// test.kt:27 stringSwitch
// test.kt:32 box
// test.kt:5 stringSwitch
// test.kt:8 stringSwitch
// test.kt:12 stringSwitch
// test.kt:15 stringSwitch
// test.kt:19 stringSwitch
// test.kt:24 stringSwitch
// test.kt:27 stringSwitch
// test.kt:33 box
// test.kt:5 stringSwitch
// test.kt:9 stringSwitch
// test.kt:12 stringSwitch
// test.kt:16 stringSwitch
// test.kt:19 stringSwitch
// test.kt:25 stringSwitch
// test.kt:27 stringSwitch
// test.kt:34 box
