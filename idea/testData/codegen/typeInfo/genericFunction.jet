class Foo() { }
class Bar() { }

fun isInstance<T>(obj: Any?) = obj is T

fun box(): String {
  if (! isInstance<Foo>(Foo())) return "fail 1"
  if (isInstance<Bar>(Foo())) return "fail 2"
  return "OK"
}
