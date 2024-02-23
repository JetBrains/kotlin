class I { }

operator fun I.invoke(): E = E()
operator fun I.invoke(x: Int): E = E()
operator fun I.invoke(x: Int, y: Int): E = E()

// operators over variables

class A { }
var a = A()

val A.inc: I get() = I()
fun useInc() { a<!NOT_FUNCTION_AS_OPERATOR, RESULT_TYPE_MISMATCH!>++<!> }

val A.dec: I get() = I()
fun useDec() { a<!NOT_FUNCTION_AS_OPERATOR, RESULT_TYPE_MISMATCH!>--<!> }

val A.plusAssign: I get() = I()
fun usePlusAssign() { a <!NOT_FUNCTION_AS_OPERATOR!>+=<!> 1 }

val A.minusAssign: I get() = I()
fun useMinusAssign() { a <!NOT_FUNCTION_AS_OPERATOR!>-=<!> 1 }

val A.timesAssign: I get() = I()
fun useTimesAssign() { a <!NOT_FUNCTION_AS_OPERATOR!>*=<!> 1 }

val A.divAssign: I get() = I()
fun useDivAssign() { a <!NOT_FUNCTION_AS_OPERATOR!>/=<!> 1 }

val A.remAssign: I get() = I()
fun useRemAssign() { a <!NOT_FUNCTION_AS_OPERATOR!>%=<!> 1 }

// operators over values

class E: Iterator<Int> {
    override fun hasNext(): Boolean = false
    override fun next(): Int = 0
}

val e = E()

val E.unaryPlus: I get() = I()
val useUnaryPlus = <!NOT_FUNCTION_AS_OPERATOR!>+<!>e

val E.unaryMinus: I get() = I()
val useUnaryMinus = <!NOT_FUNCTION_AS_OPERATOR!>-<!>e

val E.not: I get() = I()
val useNot = <!NOT_FUNCTION_AS_OPERATOR!>!<!>e

val E.plus: I get() = I()
val usePlus = e <!NOT_FUNCTION_AS_OPERATOR!>+<!> 1

val E.minus: I get() = I()
val useMinus = e <!NOT_FUNCTION_AS_OPERATOR!>-<!> 1

val E.times: I get() = I()
val useTimes = e <!NOT_FUNCTION_AS_OPERATOR!>*<!> 1

val E.div: I get() = I()
val useDiv = e <!NOT_FUNCTION_AS_OPERATOR!>/<!> 1

val E.rem: I get() = I()
val useRem = e <!NOT_FUNCTION_AS_OPERATOR!>%<!> 1

val E.get: I get() = I()
val useGet = <!NOT_FUNCTION_AS_OPERATOR!>e[1]<!>

val E.set: I get() = I()
fun useSet() { <!NOT_FUNCTION_AS_OPERATOR!>e[1]<!> = 3 }

val E.contains: I get() = I()
val useContains = 1 <!NOT_FUNCTION_AS_OPERATOR!>in<!> e
val useNotContains = 1 <!NOT_FUNCTION_AS_OPERATOR, NOT_FUNCTION_AS_OPERATOR!>!in<!> e

val E.invoke: I get() = I()
val useInvoke = <!NONE_APPLICABLE!>e<!>()

val E.rangeTo: I get() = I()
val useRangeTo = e <!NOT_FUNCTION_AS_OPERATOR!>..<!> 3

val E.rangeUntil: I get() = I()
val useRangeUntil = e <!NOT_FUNCTION_AS_OPERATOR!>..<<!> 3

val E.compareTo: I get() = I()
val useCompareTo = e <!NOT_FUNCTION_AS_OPERATOR!>><!> 2

val E.iterator: I get() = I()
fun useIterator() {
    for (x in <!NOT_FUNCTION_AS_OPERATOR!>e<!>) { }
}

class P { }

val A.getValue: P get() = P()
val A.setValue: P get() = P()

operator fun P.invoke(thisRef: Any?, property: Any?): Int = 0
operator fun P.invoke(thisRef: Any?, property: Any?, newValue: Int) { }
fun useDelegate() {
    val m by <!NOT_FUNCTION_AS_OPERATOR!>a<!>
    var n by <!NOT_FUNCTION_AS_OPERATOR, NOT_FUNCTION_AS_OPERATOR!>a<!>
}

val E.component1: I get() = I()
val E.component2: I get() = I()
fun useComponentN() {
    val (<!NOT_FUNCTION_AS_OPERATOR!>x<!>, <!NOT_FUNCTION_AS_OPERATOR!>y<!>) = e
}
