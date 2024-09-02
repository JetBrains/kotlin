
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

// EXPECTATIONS JVM_IR
// test.kt:30 box
// test.kt:5 stringSwitch
// test.kt:6 stringSwitch
// test.kt:5 stringSwitch
// test.kt:12 stringSwitch
// test.kt:13 stringSwitch
// test.kt:12 stringSwitch
// test.kt:20 stringSwitch
// test.kt:19 stringSwitch
// test.kt:22 stringSwitch
// test.kt:19 stringSwitch
// test.kt:27 stringSwitch
// test.kt:31 box
// test.kt:5 stringSwitch
// test.kt:7 stringSwitch
// test.kt:5 stringSwitch
// test.kt:12 stringSwitch
// test.kt:14 stringSwitch
// test.kt:12 stringSwitch
// test.kt:20 stringSwitch
// test.kt:19 stringSwitch
// test.kt:23 stringSwitch
// test.kt:19 stringSwitch
// test.kt:27 stringSwitch
// test.kt:32 box
// test.kt:5 stringSwitch
// test.kt:8 stringSwitch
// test.kt:5 stringSwitch
// test.kt:12 stringSwitch
// test.kt:15 stringSwitch
// test.kt:12 stringSwitch
// test.kt:20 stringSwitch
// test.kt:19 stringSwitch
// test.kt:24 stringSwitch
// test.kt:19 stringSwitch
// test.kt:27 stringSwitch
// test.kt:33 box
// test.kt:5 stringSwitch
// test.kt:9 stringSwitch
// test.kt:5 stringSwitch
// test.kt:12 stringSwitch
// test.kt:16 stringSwitch
// test.kt:12 stringSwitch
// test.kt:20 stringSwitch
// test.kt:19 stringSwitch
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

// EXPECTATIONS WASM
// test.kt:30 $box (17, 17, 17, 4)
// test.kt:6 $stringSwitch (8, 13, 13, 13, 13, 8, 20, 8, 8, 8)
// test.kt:5 $stringSwitch (4, 4, 4, 4)
// test.kt:12 $stringSwitch (19, 4, 19, 4, 19, 4, 19, 4)
// test.kt:13 $stringSwitch (8, 15, 8, 8, 8)
// test.kt:20 $stringSwitch (9, 9, 9, 9)
// test.kt:22 $stringSwitch (8, 15, 8, 8, 8)
// test.kt:19 $stringSwitch (4, 4, 4, 4)
// test.kt:27 $stringSwitch (1, 1, 1, 1)
// test.kt:31 $box (17, 17, 17, 4)
// test.kt:7 $stringSwitch (8, 13, 13, 13, 13, 8, 21)
// test.kt:14 $stringSwitch (8, 16)
// test.kt:23 $stringSwitch (8, 16)
// test.kt:32 $box (17, 17, 17, 4)
// test.kt:8 $stringSwitch (8, 13, 13, 13, 13, 8, 22)
// test.kt:15 $stringSwitch (8, 17)
// test.kt:24 $stringSwitch (8, 17)
// test.kt:33 $box (17, 17, 17, 4)
// test.kt:9 $stringSwitch
// test.kt:16 $stringSwitch
// test.kt:25 $stringSwitch
// test.kt:34 $box
