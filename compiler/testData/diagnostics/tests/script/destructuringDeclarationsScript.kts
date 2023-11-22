// !WITH_NEW_INFERENCE
// IGNORE_REVERSED_RESOLVE
// IGNORE_DIAGNOSTIC_API
// DUMP_CFG: LEVELS
// KT-62840
val (a1, a2) = A()
val (b1: Int, b2: Int) = <!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>A()<!>
val (c1) = <!UNRESOLVED_REFERENCE!>unresolved<!>

<!WRONG_MODIFIER_TARGET!>private<!> val (d1) = A()

val (e1, _) = A()

a1
a2
e1

class A {
    operator fun component1() = 1
    operator fun component2() = ""
}