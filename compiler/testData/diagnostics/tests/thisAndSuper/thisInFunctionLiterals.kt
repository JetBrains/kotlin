// !CHECK_TYPE

class A(val a:Int) {
  inner class B() {
    fun Byte.xx() : Double.() -> Any {
      checkSubtype<Byte>(this)
      val <!UNUSED_VARIABLE!>a<!>: Double.() -> Unit = {
        checkSubtype<Double>(this)
        checkSubtype<Byte>(this@xx)
        checkSubtype<B>(this@B)
        checkSubtype<A>(this@A)
      }
      val <!UNUSED_VARIABLE!>b<!>: Double.() -> Unit = a@{ checkSubtype<Double>(this@a) + checkSubtype<Byte>(this@xx) }
      val <!UNUSED_VARIABLE!>c<!> = a@{ -> <!NO_THIS!>this@a<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!> checkSubtype<Byte>(this@xx) }
      return (a@{checkSubtype<Double>(this@a) + checkSubtype<Byte>(this@xx)})
    }
  }
}