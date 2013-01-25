val Int.foo: String = "OK"
  get() {
    return $foo
  }

fun box(): String {
   return 1.foo
}