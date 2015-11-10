var x : Int <caret>by Baz()

interface Foo {
  fun getValue(p1: Any?, p2: Any?): Int = 1
}

class Baz: Foo

// REF: (in Foo).getValue(Any?, Any?)