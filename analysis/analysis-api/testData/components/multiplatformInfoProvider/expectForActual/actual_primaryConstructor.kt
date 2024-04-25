// LANGUAGE: +MultiPlatformProjects
// MODULE: commonMain
// FILE: Common.kt

package sample
expect class Foo(n: Int)

// MODULE: androidMain()()(commonMain)
// FILE: JvmAndroid.kt

package sample
actual class Foo<caret>(n: Int) {
    val k = n
}