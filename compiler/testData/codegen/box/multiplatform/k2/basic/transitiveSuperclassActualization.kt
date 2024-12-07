// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-59356

// MODULE: common
// FILE: common.kt
open class A {
    open fun foo(): String = "Fail"
}
expect class C() : A

fun commonBox(): String {
    return C().foo()
}

// MODULE: platform()()(common)
// FILE: main.kt
open class B : A() {
    override fun foo(): String = "OK"
}

actual class C actual constructor() : B()

fun box(): String {
    return commonBox()
}
