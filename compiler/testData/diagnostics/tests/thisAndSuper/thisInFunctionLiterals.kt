class A(val a:Int) {
  inner class B() {
    fun Char.xx() : Double.() -> Any {
      this : Char
      val <!UNUSED_VARIABLE!>a<!>: Double.() -> Unit = {
        this : Double
        this@xx : Char
        this@B : B
        this@A : A
      }
      val <!UNUSED_VARIABLE!>b<!>: Double.() -> Unit = a@{ this@a : Double + this@xx : Char}
      val <!UNUSED_VARIABLE!>c<!> = a@{ -> <!NO_THIS!>this@a<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!> this@xx : Char}
      return (a@{this@a : Double + this@xx : Char})
    }
  }
}