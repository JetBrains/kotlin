trait NoC {
  <!ANONYMOUS_INITIALIZER_WITHOUT_CONSTRUCTOR!>{

  }<!>

  val a : Int get() = 1

  <!ANONYMOUS_INITIALIZER_WITHOUT_CONSTRUCTOR!>{

  }<!>
}

class WithC() {
  val x : Int
  {
    $x = 1
    <!UNRESOLVED_REFERENCE!>$y<!> = 2
    val b = x

  }

  val a : Int get() = 1

  {
    val z = <!UNRESOLVED_REFERENCE!>b<!>
    val zz = x
    val zzz = <!NO_BACKING_FIELD_CUSTOM_ACCESSORS!>$a<!>
  }
}