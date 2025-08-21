val i: Int
fun foo(): String
class A {
  abstract fun foo()
}
class B: A

// SUPPRESS_INDIVIDUAL_DIAGNOSTICS_CHECK: KT-63221