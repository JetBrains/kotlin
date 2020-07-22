package test

open class A {
  open fun foo(a: <!OTHER_ERROR!>E<!>) {}
}

class B : A() {
  override fun foo(a: <!OTHER_ERROR!>E<!>) {}
}