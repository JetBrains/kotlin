// DO_NOT_CHECK_SYMBOL_RESTORE_K1

// LANGUAGE: +MultiPlatformProjects
// MODULE: common
// FILE: common.kt
package test.pkg

expect class Foo {
    expect fun foo(param: Int = 0): Unit
}

// MODULE: jvm()()(common)
// FILE: main.kt
package test.pkg

actual class Foo {
    actual fun foo(par<caret>am: Int) = Unit
}
