var x : Int <caret>by Baz()

trait Foo {
  fun get(p1: Any?, p2: Any?): Int = 1
}

class Baz: Foo

// REF: (in Foo).get(Any?,Any?)