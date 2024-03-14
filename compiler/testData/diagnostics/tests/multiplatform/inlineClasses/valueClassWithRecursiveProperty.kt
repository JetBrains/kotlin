// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect value class <!NO_ACTUAL_FOR_EXPECT!>A<!>(val s: String)

expect value class <!NO_ACTUAL_FOR_EXPECT!>B<!>(val s: String)

@JvmInline
value class C(val b: B)

expect value class <!NO_ACTUAL_FOR_EXPECT!>D<!>(val s: String)

interface I {
    val d: D
}

// MODULE: jvm()()(common)
// FILE: jvm.kt
@JvmInline
actual value class A(val s: String) {
    val a: A
        get() = A(s)
}

@JvmInline
actual value class B(val s: String) {
    val c: C
        get() = C(B(s))
}

@JvmInline
actual value class D(val s: String) : I {
    override val d
        get() = D(s)
}
