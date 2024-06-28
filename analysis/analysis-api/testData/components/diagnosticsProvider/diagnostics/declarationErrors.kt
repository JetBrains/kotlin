val i: Int
fun foo(): String
class A {
  abstract fun foo()
}
class B: A

// IGNORE_FIR
// Does not pass because of KT-61296
