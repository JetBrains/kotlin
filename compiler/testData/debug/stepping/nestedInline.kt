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
// test.kt:19
// test.kt:7
// test.kt:9
// 1.kt:18
// 1.kt:6
// test.kt:10
// 1.kt:14
// 1.kt:10
// 1.kt:11
// test.kt:11
// test.kt:12
// 1.kt:12
// 1.kt:14
// test.kt:13
// 1.kt:7
// 1.kt:18
// test.kt:15
// test.kt:19
// test.kt:21