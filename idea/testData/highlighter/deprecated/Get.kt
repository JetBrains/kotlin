class MyClass {}

deprecated("Use A instead") fun MyClass.get(i: MyClass): MyClass { return i }

fun test() {
  val x1 = MyClass()
  val x2 = MyClass()

  <warning descr="'fun get(i: MyClass)' is deprecated. Use A instead">x1[x2]</warning>
}