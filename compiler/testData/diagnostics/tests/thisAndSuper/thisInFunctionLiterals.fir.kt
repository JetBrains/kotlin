// CHECK_TYPE

class A(val a:Int) {
  inner class B() {
    fun Byte.xx() : Double.() -> Any {
      checkSubtype<Byte>(this)
      val a: Double.() -> Unit = {
        checkSubtype<Double>(this)
        checkSubtype<Byte>(this@xx)
        checkSubtype<B>(this@B)
        checkSubtype<A>(this@A)
      }
      val b: Double.() -> Unit = a@{ checkSubtype<Double>(this@a) + checkSubtype<Byte>(this@xx) }
      val c = a@{ -> this<!UNRESOLVED_LABEL!>@a<!> + checkSubtype<Byte>(this@xx) }
      return (a@{checkSubtype<Double>(this@a) + checkSubtype<Byte>(this@xx)})
    }
  }
}
