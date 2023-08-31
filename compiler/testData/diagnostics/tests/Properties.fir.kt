var x : Int = 1 + <!UNINITIALIZED_VARIABLE!>x<!>
   get() : Int = 1
   set(value : <!WRONG_SETTER_PARAMETER_TYPE!>Long<!>) {
      field = value.toInt()
      field = <!ASSIGNMENT_TYPE_MISMATCH!>1.toLong()<!>
    }

 val xx : Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1 + x<!>
   get() : Int = 1
   <!VAL_WITH_SETTER!>set(value : <!WRONG_SETTER_PARAMETER_TYPE!>Long<!>) {}<!>

  val p : Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1<!>
    get() = 1

class Test() {
    var a : Int = 111
    var b : Int = 222
        get() = field
        set(x) {a = x; field = x}

   public val i = 1
}
