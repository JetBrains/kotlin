// IGNORE_BACKEND_K2_LIGHT_TREE: JVM_IR
//   Reason: KT-56755
// FILE: test.kt

fun box() {
    val a: Int? = 0
    when (a) {
        1 -> {
            1 + 1
        }
        2 -> {
            2 + 1
        }
        else -> {
            0 + 0
        }
    }
}

// EXPECTATIONS JVM JVM_IR
// test.kt:6 box
// test.kt:7 box
// test.kt:8 box
// test.kt:11 box
// test.kt:15 box
// test.kt:18 box

// EXPECTATIONS JS_IR
// test.kt:6 box
// test.kt:7 box
// test.kt:7 box
// test.kt:18 box
