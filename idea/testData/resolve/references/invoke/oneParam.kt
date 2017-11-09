class Foo {
  fun invoke(vararg a: Any) {}
}

fun test(f: Foo) {
  f<caret>(1)
}

// REF: (in Foo).invoke(vararg Any)