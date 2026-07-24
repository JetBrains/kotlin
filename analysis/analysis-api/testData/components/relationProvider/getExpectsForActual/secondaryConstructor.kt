// LANGUAGE: +MultiPlatformProjects
// constructor: sample/Foo.init(text)

// MODULE: common
// TARGET_PLATFORM: Common
// FILE: Common.kt

package sample

expect class Foo(n: Int) {
    constructor(text: String)
}

// MODULE: jvm()()(common)
// TARGET_PLATFORM: JVM
// FILE: Jvm.kt

package sample

actual class Foo actual constructor(n: Int) {
    <expr>actual constructor(text: String) : this(text.length)</expr>
}
