class Bar(val name: String)

abstract class Foo {
  public abstract fun foo(): String
}

fun box(): String {
    return object: Foo() {
      inner data class NestedFoo(val bar: Bar)

      override fun foo(): String {
        return NestedFoo(Bar("Fail")).copy(bar = Bar("OK")).bar.name
      }
    }.foo()
}