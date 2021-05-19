var x : Int <caret>by Baz()

interface Foo {
  operator fun getValue(p1: Any?, p2: Any?): Int = 1
}

interface Bar {
  operator fun getValue(p1: Any?, p2: Any?): Int
}

interface Zoo {
  operator fun setValue(p1: Any?, p2: Any?, p3: Any?)
}

class Baz: Foo, Bar, Zoo

// MULTIRESOLVE
// REF: (in Bar).getValue(Any?, Any?)
// REF: (in Foo).getValue(Any?, Any?)
// REF: (in Zoo).setValue(Any?, Any?, Any?)
