// JAVA_SOURCES: SelfType.java
// JSPECIFY_STATE strict

fun main(ak: AK, akn: AKN, bk: BK, ck: CK, ckn: CKN): Unit {
    ak.foo(ak)
    // jspecify_nullness_mismatch
    ak.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)

    // jspecify_nullness_mismatch
    akn.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)

    bk.foo(bk)
    // jspecify_nullness_mismatch
    bk.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)

    ck.foo(ck)
    // jspecify_nullness_mismatch
    ck.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)

    // jspecify_nullness_mismatch
    ckn.foo(<!NULL_FOR_NONNULL_TYPE!>null<!>)
}