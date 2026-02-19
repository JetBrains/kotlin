// KT-80856
// `analysisContextModule` should only affect the context module of a dangling file depending on `main.kt`. So we expect `main.kt`'s module
// to still be `main` regardless of the `analysisContextModule` setting.

// MODULE: main
// FILE: main.kt
// VIRTUAL_FILE_ANALYSIS_CONTEXT_MODULE: dependent
package test

open class Main

// MODULE: dependent(main)
// FILE: a.kt
package test

class A : Main()
