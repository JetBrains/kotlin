// IGNORE_BACKEND_FIR: JVM_IR
class Bar(val name: String)

abstract class Foo {
  public abstract fun foo(): String
}

fun box(): String {
    return object: Foo() {
      inner class NestedFoo(val bar: Bar) {
          fun copy(bar: Bar) = NestedFoo(bar)
      }

      override fun foo(): String {
        return NestedFoo(Bar("Fail")).copy(bar = Bar("OK")).bar.name
      }
    }.foo()
}