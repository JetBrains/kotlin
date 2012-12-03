class Bar(val name: String)

class Baz {
  inner class Foo() {
      inner data class NestedFoo(val bar: Bar)

      fun foo(): String {
          return NestedFoo(Bar("FAIL")).copy(bar = Bar("OK")).bar.name
      }
  }
}

fun box(): String {
    return Baz().Foo().foo()
}