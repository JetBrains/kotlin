// LANGUAGE: +MultiPlatformProjects
// IGNORE_HMPP: ANY
// ISSUE: KT-88191

// MODULE: common
// FILE: common.kt
enum class SomeEnum {
    A, B
}

fun test(x: SomeEnum): String {
    when (x) {
        SomeEnum.A -> {}
        SomeEnum.B -> {}
    }
    return "OK"
}

// MODULE: platform()()(common)
// FILE: platform.kt
fun box(): String = test(SomeEnum.A)
