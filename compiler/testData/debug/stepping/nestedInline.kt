
// This is same as kotlin/compiler/testData/codegen/boxInline/smap/smap.kt
// FILE: test.kt

import builders.*

inline fun test(): String {
    var res = "Fail"

    html {
        head {
            res = "OK"
        }
    }

    return res
}

fun box(): String {
    var expected = test();

    return expected
}

// FILE: 1.kt

package builders

inline fun init(init: () -> Unit) {
    init()
}

inline fun initTag2(init: () -> Unit) {
    val p = 1;
    init()
}
//{val p = initTag2(init); return p} to remove difference in linenumber processing through MethodNode and MethodVisitor should be: = initTag2(init)
inline fun head(init: () -> Unit) { val p = initTag2(init); return p}


inline fun html(init: () -> Unit) {
    return init(init)
}

// EXPECTATIONS JVM_IR
// test.kt:20 box
// test.kt:8 box
// test.kt:10 box
// 1.kt:42 box
// 1.kt:30 box
// test.kt:11 box
// 1.kt:38 box
// 1.kt:34 box
// 1.kt:35 box
// test.kt:12 box
// test.kt:13 box
// 1.kt:35 box
// 1.kt:36 box
// 1.kt:38 box
// test.kt:14 box
// 1.kt:30 box
// 1.kt:31 box
// 1.kt:42 box
// test.kt:16 box
// test.kt:20 box
// test.kt:22 box

// EXPECTATIONS JS_IR
// test.kt:8 box
// 1.kt:34 box
// 1.kt:37 box
// 1.kt:38 box
// test.kt:16 box
// test.kt:22 box

// EXPECTATIONS WASM
// test.kt:20 $box
// test.kt:8 $box (14, 14, 4)
// test.kt:10 $box
// 1.kt:42 $box (11, 4)
// 1.kt:30 $box
// test.kt:11 $box
// 1.kt:38 $box (44, 67, 60)
// 1.kt:34 $box (12, 4)
// 1.kt:35 $box
// test.kt:12 $box (18, 12)
// test.kt:16 $box (11, 4)
// test.kt:22 $box (11, 4)
