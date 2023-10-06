class I { }

operator fun I.invoke(): E = E()
operator fun I.invoke(x: Int): E = E()
operator fun I.invoke(x: Int, y: Int): E = E()

// operators over variables

class A { }
var a = A()

val A.inc: I get() = I()
fun useInc() { a<!PROPERTY_AS_OPERATOR, RESULT_TYPE_MISMATCH!>++<!> }

val A.dec: I get() = I()
fun useDec() { a<!PROPERTY_AS_OPERATOR, RESULT_TYPE_MISMATCH!>--<!> }

val A.plusAssign: I get() = I()
fun usePlusAssign() { a <!ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT, PROPERTY_AS_OPERATOR!>+=<!> 1 }

val A.minusAssign: I get() = I()
fun useMinusAssign() { a <!ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT, PROPERTY_AS_OPERATOR!>-=<!> 1 }

val A.timesAssign: I get() = I()
fun useTimesAssign() { a <!ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT, PROPERTY_AS_OPERATOR!>*=<!> 1 }

val A.divAssign: I get() = I()
fun useDivAssign() { a <!ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT, PROPERTY_AS_OPERATOR!>/=<!> 1 }

val A.remAssign: I get() = I()
fun useRemAssign() { a <!ASSIGNMENT_OPERATOR_SHOULD_RETURN_UNIT, PROPERTY_AS_OPERATOR!>%=<!> 1 }

// operators over values

class E { }
val e = E()

val E.unaryPlus: I get() = I()
val useUnaryPlus = <!PROPERTY_AS_OPERATOR!>+<!>e

val E.unaryMinus: I get() = I()
val useUnaryMinus = <!PROPERTY_AS_OPERATOR!>-<!>e

val E.not: I get() = I()
val useNot = <!PROPERTY_AS_OPERATOR!>!<!>e

val E.plus: I get() = I()
val usePlus = e <!PROPERTY_AS_OPERATOR!>+<!> 1

val E.minus: I get() = I()
val useMinus = e <!PROPERTY_AS_OPERATOR!>-<!> 1

val E.times: I get() = I()
val useTimes = e <!PROPERTY_AS_OPERATOR!>*<!> 1

val E.div: I get() = I()
val useDiv = e <!PROPERTY_AS_OPERATOR!>/<!> 1

val E.rem: I get() = I()
val useRem = e <!PROPERTY_AS_OPERATOR!>%<!> 1

val E.get: I get() = I()
val useGet = <!PROPERTY_AS_OPERATOR!>e[1]<!>

val E.set: I get() = I()
fun useSet() { <!PROPERTY_AS_OPERATOR!>e[1]<!> = 3 }

val E.contains: I get() = I()
val useContains = 1 <!PROPERTY_AS_OPERATOR, RESULT_TYPE_MISMATCH!>in<!> e
val useNotContains = 1 <!PROPERTY_AS_OPERATOR, RESULT_TYPE_MISMATCH!>!in<!> e

val E.invoke: I get() = I()
val useInvoke = <!DEBUG_INFO_MISSING_UNRESOLVED, FUNCTION_EXPECTED!>e<!>()

val E.rangeTo: I get() = I()
val useRangeTo = e <!PROPERTY_AS_OPERATOR!>..<!> 3

val E.rangeUntil: I get() = I()
val useRangeUntil = e <!PROPERTY_AS_OPERATOR!>..<<!> 3

val E.compareTo: I get() = I()
val useCompareTo = e <!COMPARE_TO_TYPE_MISMATCH, PROPERTY_AS_OPERATOR!>><!> 2
