var x : Int = 1 + <!UNINITIALIZED_VARIABLE!>x<!>
   get() : Int = 1
   set(value : <!WRONG_SETTER_PARAMETER_TYPE!>Long<!>) {
      $x = value.toInt()
      $x = <!TYPE_MISMATCH!>1.toLong()<!>
    }

 val xx : Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1 + x<!>
   get() : Int = 1
   <!VAL_WITH_SETTER!>set(value : <!WRONG_SETTER_PARAMETER_TYPE!>Long<!>) {}<!>

  val p : Int = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>1<!>
    get() = 1

class Test() {
    var a : Int = 111
    var b : Int get() = $a; set(x) {a = x; $a = x}

    init {
    <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$b<!> = $a
    $a = <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$b<!>
    a = <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$b<!>
   }

   fun f() {
    <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$b<!> = $a
    a = <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$b<!>
   }
   <!PUBLIC_MEMBER_SHOULD_SPECIFY_TYPE!>public val i<!> = 1
}