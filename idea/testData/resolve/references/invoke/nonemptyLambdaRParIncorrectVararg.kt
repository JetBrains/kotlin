class Foo {
  fun invoke(vararg a: Any) {}
}

fun test(f: Foo) {
  f() { 1<caret>}
}

// REF: (in Foo).invoke(vararg Any)