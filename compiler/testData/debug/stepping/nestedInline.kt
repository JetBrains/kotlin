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

// LINENUMBERS
// test.kt:19 box
// test.kt:7 box
// test.kt:9 box
// 1.kt:18 box
// 1.kt:6 box
// test.kt:10 box
// 1.kt:14 box
// 1.kt:10 box
// 1.kt:11 box
// test.kt:11 box
// test.kt:12 box
// 1.kt:12 box
// 1.kt:14 box
// test.kt:13 box
// 1.kt:7 box
// 1.kt:18 box
// test.kt:15 box
// test.kt:19 box
// test.kt:21 box
