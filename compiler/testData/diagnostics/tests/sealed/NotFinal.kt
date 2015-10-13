// See KT-9244

sealed class Foo {
  class Bar : Foo()
  class Baz : Foo()
}

// The following warning seems incorrect here
// "Foo is a final type, and thus a value of the type parameter is predetermined"
fun <T : Foo> doit(arg: T): T = arg