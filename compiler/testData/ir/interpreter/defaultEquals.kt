@CompileTimeCalculation
class A

@CompileTimeCalculation
fun getTheSameValue(a: Any): Any = a

@CompileTimeCalculation
fun theSameObjectEquals(value: Any): Boolean {
    return value == getTheSameValue(value) && value === getTheSameValue(value)
}

const val equals1 = A().<!EVALUATED: `false`!>equals(A())<!>
const val equals2 = <!EVALUATED: `true`!>theSameObjectEquals(A())<!>
