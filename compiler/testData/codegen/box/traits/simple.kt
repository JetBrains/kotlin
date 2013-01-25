trait SimpleClass : java.lang.Object {
    fun foo() : String = "239 " + toString ()
}

class SimpleClassImpl() : SimpleClass {
   override fun toString() = "SimpleClassImpl"
}

fun box() : String {
  val c = SimpleClassImpl()
  return if("239 SimpleClassImpl" == c.foo()) "OK" else "fail"
}
