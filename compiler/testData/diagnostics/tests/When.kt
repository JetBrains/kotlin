fun Int.foo() : Boolean = true

fun foo() : Int {
    val s = ""
    val x = 1
    when (x) {
      is <!INCOMPATIBLE_TYPES!>String<!> -> 1
      !is Int -> 1
      is Any? -> 1
      <!INCOMPATIBLE_TYPES!>s<!> -> 1
      1 -> 1
      1 + <!UNRESOLVED_REFERENCE!>a<!> -> 1
      in 1..<!UNRESOLVED_REFERENCE!>a<!> -> 1
      !in 1..<!UNRESOLVED_REFERENCE!>a<!> -> 1
      // Commented for KT-621 .<!!UNRESOLVED_REFERENCE!>a<!!> => 1
      // Commented for KT-621 .equals(1).<!!UNRESOLVED_REFERENCE!>a<!!> => 1
      // Commented for KT-621 <!UNNECESSARY_SAFE_CALL!!>?.<!!>equals(1) => 1
      is * -> 1
    }

    // Commented for KT-621
    // return when (<!!USELESS_ELVIS!>x<!!>?:null) {
    //  <!!UNSAFE_CALL!!>.<!!>foo() => 1
    //  .equals(1) => 1
    //  ?.equals(1).equals(2) => 1
    // }
    return 0
}

val _type_test : Int = foo() // this is needed to ensure the inferred return type of foo()

fun test() {
  val x = 1;
  val s = "";

  <!NO_ELSE_IN_WHEN!>when<!> (x) {
    <!INCOMPATIBLE_TYPES!>s<!> -> 1
    is <!INCOMPATIBLE_TYPES!>""<!> -> 1
    x -> 1
    is 1 -> 1
    is <!TYPE_MISMATCH_IN_TUPLE_PATTERN!>#(1, 1)<!> -> 1
  }

  val z = #(1, 1)

  <!NO_ELSE_IN_WHEN!>when<!> (z) {
    is #(*, *) -> 1
    is #(*, 1) -> 1
    is #(1, 1) -> 1
    is #(1, <!INCOMPATIBLE_TYPES!>"1"<!>) -> 1
    is <!TYPE_MISMATCH_IN_TUPLE_PATTERN!>#(1, "1", *)<!> -> 1
    is boo  #(1, <!INCOMPATIBLE_TYPES!>"a"<!>, *) -> 1
    is boo  <!TYPE_MISMATCH_IN_TUPLE_PATTERN!>#(1, *)<!> -> 1
  }

  when (z) {
    <!ELSE_MISPLACED_IN_WHEN!>else<!> -> 1
    <!UNREACHABLE_CODE!>#(1, 1) -> 2<!>
  }

  when (z) {
    else -> 1
  }
}

val #(Int, Int).boo : #(Int, Int, Int) = #(1, 1, 1)
