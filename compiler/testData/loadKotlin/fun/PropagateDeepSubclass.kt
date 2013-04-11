package test

trait A {
  fun foo() {}

  fun bar() {}
}

open class B : A {
}

class C : B() {
  override fun bar() {}
}