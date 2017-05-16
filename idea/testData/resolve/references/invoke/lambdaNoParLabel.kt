class Foo {
  fun invoke(vararg a: Any) {}
}

fun test(f: Foo) {
  f f@ <caret>{}
}

// REF: (in Foo).invoke(vararg Any)