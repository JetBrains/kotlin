// JAVA_SOURCES: SelfType.java

fun main(ak: AK, akn: AKN, bk: BK, ck: CK, ckn: CKN): Unit {
    ak.foo(ak)
    // jspecify_nullness_mismatch
    ak.foo(null)

    // jspecify_nullness_mismatch
    akn.foo(null)

    bk.foo(bk)
    // jspecify_nullness_mismatch
    bk.foo(null)

    ck.foo(ck)
    // jspecify_nullness_mismatch
    ck.foo(null)

    // jspecify_nullness_mismatch
    ckn.foo(null)
}