fun Int.foo() : Boolean = true

fun foo() : Int {
    val s = ""
    val x = 1
    when (x) {
      is <!INCOMPATIBLE_TYPES!>String<!> -> 1
      !is Int -> 1
      is Any<!USELESS_NULLABLE_CHECK!>?<!> -> 1
      <!INCOMPATIBLE_TYPES!>s<!> -> 1
      1 -> 1
      1 + <!UNRESOLVED_REFERENCE!>a<!> -> 1
      in 1..<!UNRESOLVED_REFERENCE!>a<!> -> 1
      !in 1..<!UNRESOLVED_REFERENCE!>a<!> -> 1
      else -> 1
    }

    return 0
}

val _type_test : Int = foo() // this is needed to ensure the inferred return type of foo()

fun test() {
  val x = 1;
  val s = "";

  <!NO_ELSE_IN_WHEN!>when<!> (x) {
    <!INCOMPATIBLE_TYPES!>s<!> -> 1
    <!INCOMPATIBLE_TYPES!>""<!> -> 1
    x -> 1
    1 -> 1
  }

  val z = 1

  when (z) {
    <!ELSE_MISPLACED_IN_WHEN!>else<!> -> 1
    <!UNREACHABLE_CODE!>1 -> 2<!>
  }

  when (z) {
    else -> 1
  }
}