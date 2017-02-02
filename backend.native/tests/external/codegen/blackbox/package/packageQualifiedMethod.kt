package Foo
  fun bar() = 610

fun box(): String {
  return if (Foo.bar() == 610) "OK" else "fail"
}
