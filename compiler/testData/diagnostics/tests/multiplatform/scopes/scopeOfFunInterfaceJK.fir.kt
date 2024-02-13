// LANGUAGE: +MultiPlatformProjects
// ISSUE: KT-58845
// IGNORE_DIAGNOSTIC_API
// IGNORE_REVERSED_RESOLVE

// MODULE: common
// FILE: common.kt
expect interface I1

expect interface I2

<!FUN_INTERFACE_WRONG_COUNT_OF_ABSTRACT_MEMBERS!>fun<!> interface F1 : I1 {
    fun foo()
}

<!FUN_INTERFACE_ABSTRACT_METHOD_WITH_TYPE_PARAMETERS!>fun<!> interface F2 : I2 {}

// MODULE: jvm()()(common)
// FILE: J1.java
public interface J1 {
    public void bar();
}

// FILE: J2.java
public interface J2 {
    public <T> T bar();
}

// FILE: main.kt
actual interface I1 : J1 {}

actual interface I2 : J2 {}
