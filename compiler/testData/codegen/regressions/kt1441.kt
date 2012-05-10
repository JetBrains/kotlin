class Foo {
  var rnd = 10

  public fun equals(that : Any) : Boolean = that is Foo && (that.rnd == rnd)
}

fun box() : String {
  val a = Foo()
  val b = Foo()
  if (!a.identityEquals(a)) return "fail 1"
  if (!b.identityEquals(b)) return "fail 2"
  if (b.identityEquals(a)) return "fail 3"
  if (a.identityEquals(b)) return "fail 4"
  if( a !=b ) return "fail5"
  return "OK"
}
