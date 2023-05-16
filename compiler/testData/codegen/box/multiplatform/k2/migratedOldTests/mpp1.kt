// !LANGUAGE: +MultiPlatformProjects
// TARGET_BACKEND: JS_IR
// TARGET_BACKEND: NATIVE
// TARGET_BACKEND: WASM
// TARGET_BACKEND: JVM_IR

// IGNORE_BACKEND_K1: ANY

// MODULE: common
// FILE: common.kt

expect class A {
    constructor()

    inner class B {
        fun fourtyTwo(): Int

        constructor()
    }
}

expect fun seventeen(): Int

// MODULE: actual()()(common)
// FILE: actual.kt

actual fun seventeen() = 17
actual class A {
    actual inner class B actual constructor() {
        actual fun fourtyTwo() = 42
    }
}


fun box(): String {
    val fourtyTwo = A().B().fourtyTwo()
    if (fourtyTwo != 42)
        return "fourtyTwo is wrongly $fourtyTwo"

    val seventeen = seventeen()
    if (seventeen != 17)
        return "seventeen is wrongly $seventeen"

    return "OK"
}
