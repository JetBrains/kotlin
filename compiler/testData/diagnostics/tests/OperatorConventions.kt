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

fun usePlusAssign() { A <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>+=<!> 1 }
fun useMinusAssign() { A <!RESOLUTION_TO_CLASSIFIER!>-=<!> 1 }
fun useTimesAssign() { A <!RESOLUTION_TO_CLASSIFIER!>*=<!> 1 }
fun useDivAssign() { A <!RESOLUTION_TO_CLASSIFIER!>/=<!> 1 }
fun useRemAssign() { A <!RESOLUTION_TO_CLASSIFIER!>%=<!> 1 }
val useUnaryPlus = <!RESOLUTION_TO_CLASSIFIER!>+<!>A
val useUnaryMinus = <!RESOLUTION_TO_CLASSIFIER!>-<!>A
val useNot = <!RESOLUTION_TO_CLASSIFIER!>!<!>A
val usePlus = A <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>+<!> 1
val useMinus = A <!RESOLUTION_TO_CLASSIFIER!>-<!> 1
val useTimes = A <!RESOLUTION_TO_CLASSIFIER!>*<!> 1
val useDiv = A <!RESOLUTION_TO_CLASSIFIER!>/<!> 1
val useRem = A <!RESOLUTION_TO_CLASSIFIER!>%<!> 1
val useGet = <!RESOLUTION_TO_CLASSIFIER!>A<!NO_GET_METHOD!>[1]<!><!>
fun useSet() { <!RESOLUTION_TO_CLASSIFIER!>A<!NO_SET_METHOD!>[1]<!><!> = 3 }
val useContains = 1 <!RESOLUTION_TO_CLASSIFIER!>in<!> A
val useNotContains = 1 <!RESOLUTION_TO_CLASSIFIER!>!in<!> A
val useInvoke = <!FUNCTION_EXPECTED!>A<!>()
val useRangeTo = A <!RESOLUTION_TO_CLASSIFIER!>..<!> 3
val useRangeUntil = A <!RESOLUTION_TO_CLASSIFIER!>..<<!> 3
val useCompareTo = A <!RESOLUTION_TO_CLASSIFIER!>><!> 2
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
    val m by D
    var n by <!DELEGATE_SPECIAL_FUNCTION_MISSING!>D<!>
}

fun resolveToExtension() {
    val (<!PROPERTY_AS_OPERATOR!>uwu<!>) = X() // KT-65760
}
