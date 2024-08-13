// ISSUE: KT-65881, KT-65760

class I: Iterator<Int> {
    override fun hasNext(): Boolean = false
    override fun next(): Int = 0
}

object A {
    object plusAssign {
        operator fun invoke(x: Int): A = A
    }
    object minusAssign {
        operator fun invoke(x: Int): A = A
    }
    object timesAssign {
        operator fun invoke(x: Int): A = A
    }
    object divAssign {
        operator fun invoke(x: Int): A = A
    }
    object remAssign {
        operator fun invoke(x: Int): A = A
    }
    object unaryPlus {
        operator fun invoke(): A = A
    }
    object unaryMinus {
        operator fun invoke(): A = A
    }
    object not {
        operator fun invoke(): A = A
    }
    object plus {
        operator fun invoke(x: Int): A = A
    }
    object minus {
        operator fun invoke(x: Int): A = A
    }
    object times {
        operator fun invoke(x: Int): A = A
    }
    object div {
        operator fun invoke(x: Int): A = A
    }
    object rem {
        operator fun invoke(x: Int): A = A
    }
    object get {
        operator fun invoke(x: Int): A = A
    }
    object set {
        operator fun invoke(x: Int, y: Int): A = A
    }
    object contains {
        operator fun invoke(x: Int): A = A
    }
    object invoke {
        operator fun invoke(): A = A
    }
    object rangeTo {
        operator fun invoke(x: Int): A = A
    }
    object rangeUntil {
        operator fun invoke(x: Int): A = A
    }
    object compareTo {
        operator fun invoke(x: Int): A = A
    }
    object iterator {
        operator fun invoke(): I = I()
    }
    object component1 {
        operator fun invoke(): A = A
    }
    object component2 {
        operator fun invoke(): A = A
    }
}

fun usePlusAssign() { A <!NOT_FUNCTION_AS_OPERATOR!>+=<!> 1 }
fun useMinusAssign() { A <!NOT_FUNCTION_AS_OPERATOR!>-=<!> 1 }
fun useTimesAssign() { A <!NOT_FUNCTION_AS_OPERATOR!>*=<!> 1 }
fun useDivAssign() { A <!NOT_FUNCTION_AS_OPERATOR!>/=<!> 1 }
fun useRemAssign() { A <!NOT_FUNCTION_AS_OPERATOR!>%=<!> 1 }
val useUnaryPlus = <!NOT_FUNCTION_AS_OPERATOR!>+<!>A
val useUnaryMinus = <!NOT_FUNCTION_AS_OPERATOR!>-<!>A
val useNot = <!NOT_FUNCTION_AS_OPERATOR!>!<!>A
val usePlus = A <!NOT_FUNCTION_AS_OPERATOR!>+<!> 1
val useMinus = A <!NOT_FUNCTION_AS_OPERATOR!>-<!> 1
val useTimes = A <!NOT_FUNCTION_AS_OPERATOR!>*<!> 1
val useDiv = A <!NOT_FUNCTION_AS_OPERATOR!>/<!> 1
val useRem = A <!NOT_FUNCTION_AS_OPERATOR!>%<!> 1
val useGet = <!NOT_FUNCTION_AS_OPERATOR!>A[1]<!>
fun useSet() { <!NOT_FUNCTION_AS_OPERATOR!>A[1]<!> = 3 }
val useContains = 1 <!NOT_FUNCTION_AS_OPERATOR!>in<!> A
val useNotContains = 1 <!NOT_FUNCTION_AS_OPERATOR, UNRESOLVED_REFERENCE!>!in<!> A
val useInvoke = <!UNRESOLVED_REFERENCE!>A<!>()
val useRangeTo = A <!NOT_FUNCTION_AS_OPERATOR!>..<!> 3
val useRangeUntil = A <!NOT_FUNCTION_AS_OPERATOR!>..<<!> 3
val useCompareTo = A <!NOT_FUNCTION_AS_OPERATOR!>><!> 2
fun useIterator() {
    for (x in <!ITERATOR_MISSING!>A<!>) { }
}
fun useComponentN() {
    val (x, y) = <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING!>A<!>
}

object D {
    object getValue {
        operator fun invoke(thisRef: Any?, property: Any?): Int = 0
    }
    object setValue {
        operator fun invoke(thisRef: Any?, property: Any?, newValue: Int) { }
    }
}

class X {
    val component1 = { "UwU" }
}

operator fun X.<!EXTENSION_FUNCTION_SHADOWED_BY_MEMBER_PROPERTY_WITH_INVOKE!>component1<!>(): String = "Not UwU"

fun useDelegate() {
    val m by <!DELEGATE_SPECIAL_FUNCTION_MISSING!>D<!>
    var n by <!DELEGATE_SPECIAL_FUNCTION_MISSING, DELEGATE_SPECIAL_FUNCTION_MISSING!>D<!>
}

fun resolveToExtension() {
    val (uwu) = X() // KT-65760
}
