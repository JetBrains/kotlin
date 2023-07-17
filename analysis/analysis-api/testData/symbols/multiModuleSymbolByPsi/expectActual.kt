// !LANGUAGE: +MultiPlatformProjects
// MODULE: commonMain
// FILE: Common.kt

package sample
expect object A

expect class B {
    class Nested
}

expect class C {
    class Nested
}

// MODULE: androidMain(commonMain)
// FILE: JvmAndroid.kt

package sample
actual object A

actual class B {
    actual class Nested
}

actual typealias C = D

class D {
    class Nested
}
