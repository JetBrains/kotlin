class MyClass {}

Deprecated fun MyClass.get(i: MyClass): MyClass { return MyClass() }

fun test() {
  val x1 = MyClass()
  val x2 = MyClass()

  <info descr="'fun get(i : MyClass)' is deprecated">x1[x2]</info>
}