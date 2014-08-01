object A {
  class B
  fun foo() = 1
  object Bar{}
}

fun test<T>(a: T) {
  val c = (a as A)
    c.<error descr="[NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE] Nested class 'B' accessed via instance reference">B</error>()
}