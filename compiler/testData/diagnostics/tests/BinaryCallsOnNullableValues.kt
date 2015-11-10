class A() {
  override fun equals(other : Any?) : Boolean = false
}

fun f(): Unit {
  var x: Int? = <!VARIABLE_WITH_REDUNDANT_INITIALIZER!>1<!>
  x = null
  <!ALWAYS_NULL!>x<!> <!UNSAFE_INFIX_CALL!>+<!> 1
  <!ALWAYS_NULL!>x<!> <!UNSAFE_INFIX_CALL, INFIX_MODIFIER_REQUIRED!>plus<!> 1
  <!ALWAYS_NULL!>x<!> <!UNSAFE_INFIX_CALL!><<!> 1
  x <!UNSAFE_INFIX_CALL!>+=<!> 1

  <!ALWAYS_NULL!>x<!> == 1
  <!ALWAYS_NULL!>x<!> != 1

  <!EQUALITY_NOT_APPLICABLE!>A() == 1<!>

  <!EQUALITY_NOT_APPLICABLE!><!ALWAYS_NULL!>x<!> === "1"<!>
  <!EQUALITY_NOT_APPLICABLE!><!ALWAYS_NULL!>x<!> !== "1"<!>

  <!ALWAYS_NULL!>x<!> === 1
  <!ALWAYS_NULL!>x<!> !== 1

  <!ALWAYS_NULL!>x<!><!UNSAFE_INFIX_CALL!>..<!>2
  <!ALWAYS_NULL, TYPE_MISMATCH!>x<!> in 1..2

  val y : Boolean? = true
  <!UNUSED_EXPRESSION!>false || <!TYPE_MISMATCH!>y<!><!>
  <!UNUSED_EXPRESSION!><!TYPE_MISMATCH!>y<!> && true<!>
  <!UNUSED_EXPRESSION!><!TYPE_MISMATCH!>y<!> && <!CONSTANT_EXPECTED_TYPE_MISMATCH!>1<!><!>
}