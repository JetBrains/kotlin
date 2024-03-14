// LANGUAGE: +MultiPlatformProjects, +CustomEqualsInValueClasses
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect value class <!NO_ACTUAL_FOR_EXPECT!>A<!>(val f: () -> Boolean)

// MODULE: jvm()()(common)
// FILE: jvm.kt
@JvmInline
actual value class A(val f: () -> Boolean) {
    override fun <!INEFFICIENT_EQUALS_OVERRIDING_IN_VALUE_CLASS!>equals<!>(other: Any?): Boolean = f()
}
