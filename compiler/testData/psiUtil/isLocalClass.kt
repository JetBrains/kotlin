class A {
  class B
  object G

  default object {
    class C
    object H

    fun foo() {
      class DLocal
      object KLocal
    }
  }

  fun foo2() {
    class ELocal {
      class FLocal
    }
    object LLocal
  }
}
