class Foo() {}
class Bar() {}

class InstanceChecker<T>() {
  fun check(obj: Any?) = obj is T
}

fun box(): String {
  val checker = InstanceChecker<Foo>()
  if (!checker.check(Foo())) return "fail 1"
  if (checker.check(Bar())) return "fail 2"
  return "OK"
}
