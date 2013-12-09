class A() {
  fun equals(<!UNUSED_PARAMETER!>a<!> : Any?) : Boolean = false
}

class B() {
  fun equals(<!UNUSED_PARAMETER!>a<!> : Any?) : Boolean? = false
}

class C() {
  fun equals(<!UNUSED_PARAMETER!>a<!> : Any?) : Int = 0
}

fun f(): Unit {
  var x: Int? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>1<!>
  x = 1
  x <!UNSAFE_INFIX_CALL!>+<!> 1
  x <!UNSAFE_INFIX_CALL!>plus<!> 1
  x <!UNSAFE_INFIX_CALL!><<!> 1
  x <!UNSAFE_INFIX_CALL!>+=<!> 1

  x == 1
  x != 1

  <!EQUALITY_NOT_APPLICABLE!>A() == 1<!>
  B() <!RESULT_TYPE_MISMATCH!>==<!> 1
  C() <!RESULT_TYPE_MISMATCH!>==<!> 1

  <!EQUALITY_NOT_APPLICABLE!>x === "1"<!>
  <!EQUALITY_NOT_APPLICABLE!>x !== "1"<!>

  x === 1
  x !== 1

  x<!UNSAFE_INFIX_CALL!>..<!>2
  <!TYPE_MISMATCH!>x<!> in 1..2

  val y : Boolean? = true
  false || <!TYPE_MISMATCH!>y<!>
  <!TYPE_MISMATCH!>y<!> && true
  <!TYPE_MISMATCH!>y<!> && <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!>
}
