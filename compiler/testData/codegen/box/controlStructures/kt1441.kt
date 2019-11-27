// IGNORE_BACKEND_FIR: JVM_IR
class Foo {
  var rnd = 10

  public override fun equals(that : Any?) : Boolean = that is Foo && (that.rnd == rnd)
}

fun box() : String {
  val a = Foo()
  val b = Foo()
  if (a !== a) return "fail 1"
  if (b !== b) return "fail 2"
  if (b === a) return "fail 3"
  if (a === b) return "fail 4"
  if( a !=b ) return "fail5"
  return "OK"
}
