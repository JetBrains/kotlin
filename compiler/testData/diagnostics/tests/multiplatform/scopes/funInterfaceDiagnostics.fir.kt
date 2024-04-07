// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-58845

// MODULE: common
// FILE: common.kt
expect interface I1

expect interface I2

expect interface I3

expect interface I4

<!FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES, FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS{METADATA}!>fun<!> interface F1 : I1 {}

<!FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES, FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS{METADATA}!>fun<!> interface F2 : I2 {}

<!FUN_INTERFACE_ABSTRACT_METHOD_WITH_DEFAULT_VALUE, FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS{METADATA}!>fun<!> interface F3 : I3 {}

<!FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS, FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS{METADATA}!>fun<!> interface F4 : I4 {}

// MODULE: jvm()()(common)
// FILE: main.kt
actual interface I1 {
    val a: Int
    fun foo()
}

actual interface I2 {
    fun foo()
    val a: Int
}

actual interface I3 {
    fun foo(a: Int = 0)
}

actual interface I4 {
    fun <T> foo()
}
