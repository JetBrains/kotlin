// KJS_WITH_FULL_RUNTIME
// MODULE: lib1
// FILE: lib1.kt

fun module1() = "K"

// MODULE: lib2(lib1)
// FILE: lib2.kt

fun module2() = "O" + module1()

// MODULE: main(lib2)
// FILE: main.kt

fun box() = module2()
