var x : Int <caret>by Baz()

interface Foo {
  fun getValue(p1: Any?, p2: Any?): Int = 1
}

interface Bar {
  fun getValue(p1: Any?, p2: Any?): Int
}

interface Zoo {
  fun setValue(p1: Any?, p2: Any?, p3: Any?)
}

class Baz: Foo, Bar, Zoo

// MULTIRESOLVE
// REF: (in Bar).getValue(Any?, Any?)
// REF: (in Foo).getValue(Any?, Any?)
// REF: (in Zoo).setValue(Any?, Any?, Any?)
