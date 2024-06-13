class Foo {
 operator fun invoke(vararg a: Any) {}
}

fun test(f: Foo) {
  f(<caret>)
}

