public class MyClass: A() {
    override fun foo() {
      super<A<caret>>.foo()
    }
}

open class A {
  fun foo() {}
}