// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

@JvmInline
value class P(val n: Int)

class H @JvmExposeBoxed constructor(val p: P, val extra: Int = 10)

class S {
    val p: P
    val extra: Int

    @JvmExposeBoxed
    constructor(p: P, extra: Int = 20) {
        this.p = p
        this.extra = extra
    }
}

@JvmExposeBoxed
fun foo(p: P, x: Int = 1): Int = p.n + x

// LIGHT_ELEMENTS_NO_DECLARATION: DefaultArgumentsKt.class[foo-6HP3f5w], H.class[getP-oJBuErk], P.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], S.class[getP-oJBuErk]
