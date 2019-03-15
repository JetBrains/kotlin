//KT-750 Type inference failed: Constraint violation
fun main() {
  var i : Int? = Integer.valueOf(100)
  var s : Int? = Integer.valueOf(100)

  val o = i.sure() + s.sure()
  System.out.println(o)
}

fun <T : Any> T?.sure() : T = this!!
