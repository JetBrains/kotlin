class Foo {
  operator fun invoke(a: Any) {}
}

fun test(f: Foo) {
  f {<caret>}
}

// REF: (in Foo).invoke(Any)