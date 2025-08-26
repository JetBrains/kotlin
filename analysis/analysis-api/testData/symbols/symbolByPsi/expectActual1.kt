// LANGUAGE: +MultiPlatformProjects
// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1
// MODULE: common
// FILE: common.kt

package sample
expect object A

expect class B(s: String) {
    constructor(n: Int)

    class Nested

    fun bar()

    val n: Int
}

expect class C {
    class Nested
}

expect fun baz()

expect var m: Int

// MODULE: jvm-main()()(common)
// FILE: main.kt

package sample
actual object A

actual class B actual constructor(s: String) {
    actual constructor(n: Int) : this("")

    actual class Nested

    actual fun bar() {}

    actual val n: Int = 0
}

actual typealias C = D

class D {
    class Nested
}

actual fun baz() {}

actual var m: Int = 0