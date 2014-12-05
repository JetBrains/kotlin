package test

open class A {
  open fun foo(a: <!UNRESOLVED_REFERENCE!>E<!>) {}
}

class B : A() {
  override fun foo(a: <!UNRESOLVED_REFERENCE!>E<!>) {}
}