class A {
  class B
  object G

  companion object {
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
