class Foo {
  operator fun invoke(a: Any) {}
}

fun test(f: Foo) {
  f() { 1<caret>}
}

// REF: (in Foo).invoke(Any)