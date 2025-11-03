// ISSUE: KT-82065
// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization
// DUMP_IR_AFTER_INLINE

// MODULE: lib
// FILE: lib.kt

inline fun foo(a: Int = 1) = a + 1
inline fun bar(b: Int, c: Int = 2) = b + c

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    foo()
    foo(5)
    bar(10)
    bar(10, 20)
    return "OK"
}