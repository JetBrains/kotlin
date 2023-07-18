// !LANGUAGE: +MultiPlatformProjects
// MODULE: commonMain
// FILE: Common.kt

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

// MODULE: androidMain(commonMain)
// FILE: JvmAndroid.kt

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