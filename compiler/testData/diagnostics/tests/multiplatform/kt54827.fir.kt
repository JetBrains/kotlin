// MODULE: m1-common
// FILE: common.kt
<!INCOMPATIBLE_MATCHING{JVM}!>expect class SomeClass<T> {
    fun foo()
}<!>

// MODULE: m1-jvm()()(m1-common)
// FILE: jvm.kt
<!ACTUAL_WITHOUT_EXPECT!>actual class SomeClass {
    actual fun foo() {}
}<!>
