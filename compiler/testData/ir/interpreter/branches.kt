@CompileTimeCalculation fun ifGreaterOrEqualToZero(a: Int): Boolean {
    return if (a >= 0) {
        true
    } else {
        false
    }
}

@CompileTimeCalculation fun whenGreaterOrEqualToZero(a: Int): Boolean {
    return when {
        a >= 0 -> true
        else -> false
    }
}

@CompileTimeCalculation fun whenMultiBranch(a: Int): Int {
    return when (a) {
        1 -> -1
        2 -> -2
        3 -> -3
        4 -> -4
        else -> 0
    }
}

class A @CompileTimeCalculation constructor(@CompileTimeCalculation var a: Int)

@CompileTimeCalculation fun whenWithoutReturn(aObj: A): Int {
    when (aObj.a) {
        1 -> aObj.a = -1
        2 -> aObj.a = -2
        3 -> aObj.a = -3
        4 -> aObj.a = -4
        else -> aObj.a = 0
    }

    return aObj.a
}

@CompileTimeCalculation fun whenWithoutElse(aObj: A): Int {
    when (aObj.a) {
        1 -> aObj.a = -1
        2 -> aObj.a = -2
        3 -> aObj.a = -3
        4 -> aObj.a = -4
    }

    return aObj.a
}

const val a = <!EVALUATED: `true`!>ifGreaterOrEqualToZero(10)<!>
const val b = <!EVALUATED: `false`!>whenGreaterOrEqualToZero(-10)<!>
const val constIf = <!EVALUATED: `True`!>if (a == true) "True" else "False"<!>

const val multi1 = <!EVALUATED: `-2`!>whenMultiBranch(2)<!>
const val multi2 = <!EVALUATED: `-4`!>whenMultiBranch(4)<!>
const val multi3 = <!EVALUATED: `0`!>whenMultiBranch(10)<!>

const val c1 = <!EVALUATED: `-1`!>whenWithoutReturn(A(1))<!>
const val c2 = <!EVALUATED: `-3`!>whenWithoutReturn(A(3))<!>
const val c3 = <!EVALUATED: `0`!>whenWithoutReturn(A(10))<!>

const val d1 = <!EVALUATED: `-1`!>whenWithoutElse(A(1))<!>
const val d2 = <!EVALUATED: `-4`!>whenWithoutElse(A(4))<!>
const val d3 = <!EVALUATED: `10`!>whenWithoutElse(A(10))<!>
