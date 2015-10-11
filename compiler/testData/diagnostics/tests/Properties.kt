var x : Int = 1 + <!UNINITIALIZED_VARIABLE!>x<!>
   get() : Int = 1
   set(value : <!WRONG_SETTER_PARAMETER_TYPE!>Long<!>) {
      field = value.toInt()
      field = <!TYPE_MISMATCH!>1.toLong()<!>
    }

 val xx : Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1 + x<!>
   get() : Int = 1
   <!VAL_WITH_SETTER!>set(value : <!WRONG_SETTER_PARAMETER_TYPE!>Long<!>) {}<!>

  val p : Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1<!>
    get() = 1

class Test() {
    var a : Int = 111
    var b : Int get() = <!BACKING_FIELD_USAGE_FORBIDDEN!>$a<!>; set(x) {a = x; <!BACKING_FIELD_USAGE_FORBIDDEN!>$a<!> = x}

    init {
    <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$b<!> = <!BACKING_FIELD_USAGE_FORBIDDEN!>$a<!>
    <!BACKING_FIELD_USAGE_FORBIDDEN!>$a<!> = <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$b<!>
    a = <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$b<!>
   }

   fun f() {
    <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$b<!> = <!BACKING_FIELD_USAGE_FORBIDDEN!>$a<!>
    a = <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$b<!>
   }
   public val i = 1
}