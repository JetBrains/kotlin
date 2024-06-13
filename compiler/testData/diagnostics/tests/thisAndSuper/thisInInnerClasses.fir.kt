// CHECK_TYPE

class A(val a:Int) {

  inner class B() {
    val x = checkSubtype<B>(this@B)
    val y = checkSubtype<A>(this@A)
    val z = checkSubtype<B>(this)
    val Int.xx : Int get() = checkSubtype<Int>(this)
  }
}