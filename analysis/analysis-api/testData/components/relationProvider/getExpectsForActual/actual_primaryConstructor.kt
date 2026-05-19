// LANGUAGE: +MultiPlatformProjects
// MODULE: commonMain
// FILE: Common.kt

package sample
expect class Foo(n: Int)

// MODULE: androidMain()()(commonMain)
// FILE: JvmAndroid.kt

package sample
actual class Foo<expr>(n: Int)</expr> {
    val k = n
}
