object A {
  class B
  fun foo() = 1
  object Bar{}
}

fun test<T>(a: T) {
  val c = (a as A)
  c.<error descr="[FUNCTION_EXPECTED] Expression 'B' of type '[Package-type B]' cannot be invoked as a function. The function invoke() is not found"><error>B</error></error>()
}