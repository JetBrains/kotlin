trait NoC {
  <!ANONYMOUS_INITIALIZER_IN_TRAIT!>{

  }<!>

  val a : Int get() = 1

  <!ANONYMOUS_INITIALIZER_IN_TRAIT!>{

  }<!>
}

class WithC() {
  val x : Int
  {
    $x = 1
    <!UNRESOLVED_REFERENCE!>$y<!> = 2
    val <!UNUSED_VARIABLE!>b<!> = x

  }

  val a : Int get() = 1

  {
    val <!UNUSED_VARIABLE!>z<!> = <!UNRESOLVED_REFERENCE!>b<!>
    val <!UNUSED_VARIABLE!>zz<!> = x
    val <!UNUSED_VARIABLE!>zzz<!> = <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$a<!>
  }
}