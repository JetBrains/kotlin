var x : Int <caret>by Baz()

interface Foo {
  fun get(p1: Any?, p2: Any?): Int = 1
}

class Baz: Foo

// REF: (in Foo).get(Any?,Any?)