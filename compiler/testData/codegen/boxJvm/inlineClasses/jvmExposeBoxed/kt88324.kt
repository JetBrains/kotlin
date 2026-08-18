// WITH_STDLIB
// TARGET_BACKEND: JVM_IR

// MODULE: lib
// FILE: lib.kt
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline value class P(val n: Int)
class H @JvmExposeBoxed constructor(val p: P, val extra: Int = 10)

class S {
    val p: P
    val extra: Int

    @JvmExposeBoxed constructor(p: P, extra: Int = 20) {
        this.p = p
        this.extra = extra
    }
}

// All inline class parameters are already boxed, so no separate exposed constructor is generated
class N @JvmExposeBoxed constructor(val p: P?, val extra: Int = 30)

// MODULE: main(lib)
// FILE: usage.kt
fun box(): String {
    val defaulted = H(P(2))
    if (defaulted.p.n != 2 || defaulted.extra != 10) return "FAIL H defaulted: ${defaulted.p.n}, ${defaulted.extra}"

    val explicit = H(P(3), 4)
    if (explicit.p.n != 3 || explicit.extra != 4) return "FAIL H explicit: ${explicit.p.n}, ${explicit.extra}"

    val secondary = S(P(5))
    if (secondary.p.n != 5 || secondary.extra != 20) return "FAIL S defaulted: ${secondary.p.n}, ${secondary.extra}"

    val boxed = N(P(6))
    if (boxed.p?.n != 6 || boxed.extra != 30) return "FAIL N defaulted: ${boxed.p?.n}, ${boxed.extra}"

    return "OK"
}
