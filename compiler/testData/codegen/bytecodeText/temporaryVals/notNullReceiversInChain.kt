class A(val b: B)
class B(val c: C)
class C(val s: String)

fun test(na: A?) =
    na?.b?.c?.s

// 1 POP
// 1 ACONST_NULL

// JVM_IR_TEMPLATES
// 1 DUP
// 1 IFNULL

// JVM_TEMPLATES
// 3 DUP
// 3 IFNULL