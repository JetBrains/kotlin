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
// TestKt.box():19
// TestKt.box():7
// TestKt.box():9
// TestKt.box():18
// TestKt.box():6
// TestKt.box():10
// TestKt.box():14
// TestKt.box():10
// TestKt.box():11
// TestKt.box():11
// TestKt.box():12
// TestKt.box():12
// TestKt.box():14
// TestKt.box():13
// TestKt.box():7
// TestKt.box():18
// TestKt.box():15
// TestKt.box():19
// TestKt.box():21