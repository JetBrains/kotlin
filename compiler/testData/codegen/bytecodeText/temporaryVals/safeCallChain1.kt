class A(val b: B)
class B(val c: C)
class C(val s: String)

fun test(an: A?) = an?.b?.c?.s

// 0 ASTORE
// 3 IFNULL
// 0 IFNONNULL
// 1 ACONST_NULL
