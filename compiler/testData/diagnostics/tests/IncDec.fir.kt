// RUN_PIPELINE_TILL: FRONTEND

class IncDec() {
  operator fun inc() : IncDec = this
  operator fun dec() : IncDec = this
}

fun testIncDec() {
  var x = IncDec()
  x++
  ++x
  x--
  --x
  x = x++
  x = x--
  x = ++x
  x = --x
}

class WrongIncDec() {
  <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun inc() : Int = 1
  <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun dec() : Int = 1
}

fun testWrongIncDec() {
  var x = WrongIncDec()
  x<!INC_DEC_OPERATOR_RETURN_TYPE_MISMATCH!>++<!>
  <!INC_DEC_OPERATOR_RETURN_TYPE_MISMATCH!>++<!>x
  x<!INC_DEC_OPERATOR_RETURN_TYPE_MISMATCH!>--<!>
  <!INC_DEC_OPERATOR_RETURN_TYPE_MISMATCH!>--<!>x
}

class UnitIncDec() {
  <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun inc() : Unit {}
  <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun dec() : Unit {}
}

fun testUnitIncDec() {
  var x = UnitIncDec()
  x<!INC_DEC_OPERATOR_RETURN_TYPE_MISMATCH!>++<!>
  <!INC_DEC_OPERATOR_RETURN_TYPE_MISMATCH!>++<!>x
  x<!INC_DEC_OPERATOR_RETURN_TYPE_MISMATCH!>--<!>
  <!INC_DEC_OPERATOR_RETURN_TYPE_MISMATCH!>--<!>x
  x = x<!INC_DEC_OPERATOR_RETURN_TYPE_MISMATCH!>++<!>
  x = x<!INC_DEC_OPERATOR_RETURN_TYPE_MISMATCH!>--<!>
  x = <!INC_DEC_OPERATOR_RETURN_TYPE_MISMATCH!>++<!>x
  x = <!INC_DEC_OPERATOR_RETURN_TYPE_MISMATCH!>--<!>x
}

/* GENERATED_FIR_TAGS: assignment, classDeclaration, functionDeclaration, incrementDecrementExpression, integerLiteral,
intersectionType, localProperty, operator, primaryConstructor, propertyDeclaration, smartcast, thisExpression */
