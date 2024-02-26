// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-58845

// MODULE: common
// FILE: common.kt
expect interface <!NO_ACTUAL_FOR_EXPECT!>I1<!>

expect interface <!NO_ACTUAL_FOR_EXPECT!>I2<!>

expect interface <!NO_ACTUAL_FOR_EXPECT!>I3<!>

expect interface <!NO_ACTUAL_FOR_EXPECT!>I4<!>

<!FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES{JVM}, FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface F1 : I1 {}

<!FUN_INTERFACE_CANNOT_HAVE_ABSTRACT_PROPERTIES{JVM}, FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface F2 : I2 {}

<!FUN_INTERFACE_ABSTRACT_METHOD_WITH_DEFAULT_VALUE{JVM}, FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface F3 : I3 {}

<!FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS{JVM}, FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface F4 : I4 {}

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
