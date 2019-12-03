class Foo {
  fun isOk() = true
}

fun box(): String {
   val foo: Foo? = Foo()
   if (foo?.isOk()!!) return "OK"
   return "fail"
}
