// LANGUAGE: +MultiPlatformProjects, +CustomEqualsInValueClasses
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect value class <!NO_ACTUAL_FOR_EXPECT!>A<!>(val i: Int)

expect value class <!NO_ACTUAL_FOR_EXPECT!>B<!><T>(val i: Int)

expect value class <!NO_ACTUAL_FOR_EXPECT!>C<!>(val i: Int)

interface I1 {
    fun <T> equals(other: C): Boolean
}

expect value class <!NO_ACTUAL_FOR_EXPECT!>D<!><T>(val i: Int)

interface I2 {
    fun <T> equals(other: D<T>): Boolean
}

// MODULE: jvm()()(common)
// FILE: jvm.kt
@JvmInline
actual value class A(val i: Int) {
    fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> equals(other: A): Boolean = true
}

@JvmInline
actual value class B<T>(val i: Int) {
    fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> equals(other: <!TYPE_ARGUMENT_ON_TYPED_VALUE_CLASS_EQUALS!>B<T><!>): Boolean = true
}

@JvmInline
actual value class C(val i: Int) : I1 {
    override fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> equals(other: C): Boolean = true
}

@JvmInline
actual value class D<T>(val i: Int) : I2 {
    override fun <!TYPE_PARAMETERS_NOT_ALLOWED!><T><!> equals(other: <!TYPE_ARGUMENT_ON_TYPED_VALUE_CLASS_EQUALS!>D<T><!>): Boolean = true
}
