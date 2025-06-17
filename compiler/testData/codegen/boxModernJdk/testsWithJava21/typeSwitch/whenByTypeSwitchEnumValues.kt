// TARGET_BACKEND: JVM
// WHEN_EXPRESSIONS: INDY

// CHECK_BYTECODE_TEXT
// 1 TABLESWITCH
// 0 INVOKEDYNAMIC typeSwitch
// 0 INSTANCEOF

// It has been actually working fine with WhenMappings.class and TABLESWITCH without indy typeswitch
// but it seems to be a good place to have this test as well

enum class EnumClass {
    A, B, C
}

fun foo(x: EnumClass): Int {
    return when (x) {
        EnumClass.A -> 1
        EnumClass.B -> 2
        else -> 100
    }
}

fun box(): String {
    if (foo(EnumClass.A) != 1) return "EnumClass.A"
    if (foo(EnumClass.B) != 2) return "EnumClass.B"
    if (foo(EnumClass.C) != 100) return "EnumClass.C"

    return "OK"
}
