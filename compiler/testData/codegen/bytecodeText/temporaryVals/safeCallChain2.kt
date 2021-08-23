class A(val bn: B?)
class B(val cn: C?)
class C(val s: String)

fun test(an: A?) = an?.bn?.cn?.s

// JVM_IR_TEMPLATES
// 0 ASTORE
