val x : Int <expr>by Baz()</expr>

interface Foo {
  operator fun getValue(p1: Any?, p2: Any?): Int = 1
}

class Baz: Foo

