class Bar(val name: String)

class Baz {
  class Foo() {
      data class NestedFoo(val bar: Bar)

      fun foo(): String {
          return NestedFoo(Bar("FAIL")).copy(bar = Bar("OK")).bar.name
      }
  }
}

fun box(): String {
    return Baz().Foo().foo()
}