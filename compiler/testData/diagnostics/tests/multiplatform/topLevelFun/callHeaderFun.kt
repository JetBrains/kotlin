// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header fun foo(x: Int): Int

fun callFromCommonCode(x: Int) = foo(x)

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

impl fun foo(x: Int): Int {
    return x + 1
}

fun callFromJVM(x: Int) = foo(x)

// MODULE: m3-js(m1-common)
// FILE: js.kt

impl fun foo(x: Int): Int {
    return x - 1
}

fun callFromJS(x: Int) = foo(x)
