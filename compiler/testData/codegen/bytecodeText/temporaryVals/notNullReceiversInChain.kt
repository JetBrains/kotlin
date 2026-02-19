class A(val b: B)
class B(val c: C)
class C(val s: String)

fun test(na: A?) =
    na?.b?.c?.s

// 3 DUP
// 3 IFNULL
// 0 IFNONNULL
// 1 ACONST_NULL
