// !WITH_NEW_INFERENCE

fun Int.foo() : Boolean = true

fun foo() : Int {
    val s = ""
    val x = 1
    when (x) {
      is String -> 1
      !is Int -> 1
      is Any? -> 1
      is Any -> 1
      s -> 1
      1 -> 1
      1 <!AMBIGUITY!>+<!> <!UNRESOLVED_REFERENCE!>a<!> -> 1
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

  when (x) {
    s -> 1
    "" -> 1
    x -> 1
    1 -> 1
  }

  val z = 1

  when (z) {
    else -> 1
    1 -> 2
  }

  when (z) {
    else -> 1
  }
}