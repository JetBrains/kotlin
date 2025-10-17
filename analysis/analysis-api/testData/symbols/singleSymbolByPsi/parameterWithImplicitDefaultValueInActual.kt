// DO_NOT_CHECK_NON_PSI_SYMBOL_RESTORE_K1

// LANGUAGE: +MultiPlatformProjects
// MODULE: common
// FILE: common.kt
package test.pkg

expect fun foo(param: Int = 0): Unit

// MODULE: jvm()()(common)
// FILE: main.kt
package test.pkg

actual fun foo(par<caret>am: Int) = Unit
