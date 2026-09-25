// MODULE: main-common
// FILE: common.kt
package test.pkg

expect class Foo

class CommonClass {
    fun common(): Foo = TODO()
}

// MODULE: jvm()()(main-common)
// TARGET_PLATFORM: JVM
// FILE: jvm.kt
package test.pkg

actual typealias Foo = String
