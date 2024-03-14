// LANGUAGE: +MultiPlatformProjects
// WITH_STDLIB

// MODULE: common
// FILE: common.kt
expect value class A(val s: String)

expect value class B(val s: String)

@JvmInline
value class C(val b: B)

expect value class D(val s: String)

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
