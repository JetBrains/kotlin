package test

open class A {
  open fun foo(a: E) {}
}

class B : A() {
  override fun foo(a: E) {}
}
