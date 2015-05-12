var x : Int <caret>by Baz()

interface Foo {
  fun get(p1: Any?, p2: Any?): Int = 1
}

interface Bar {
  fun get(p1: Any?, p2: Any?): Int
}

interface Zoo {
  fun set(p1: Any?, p2: Any?, p3: Any?)
}

class Baz: Foo, Bar, Zoo

// MULTIRESOLVE
// REF: (in Bar).get(Any?,Any?)
// REF: (in Foo).get(Any?,Any?)
// REF: (in Zoo).set(Any?,Any?,Any?)
