// LANGUAGE: +MultiPlatformProjects
// MODULE: commonMain
// FILE: foo.kt

expect class Foo

// MODULE: androidMain(commonMain)
// FILE: foo.android.kt

actual typealias Foo = Int

// FILE: main.kt

fun b<caret>ox(): Foo {
    return 42
}
