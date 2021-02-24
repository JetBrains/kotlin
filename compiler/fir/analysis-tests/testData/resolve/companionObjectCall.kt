// UNEXPECTED BEHAVIOUR
// Issue: KT-37056
class A()

// TESTCASE NUMBER: 1
fun case1(a: A?) {
    val test = a?.let {

        Case1.invoke(it)  //resolved to private constructor

        Case1(it) //resolved to private constructor

        Case1(A()) //resolved to private constructor
    }

    Case1(A()) //resolved to private constructor
    Case1(a = A()) //resolved to private constructor
}

class Case1 private constructor(val a: String) {
    companion object {
        operator fun invoke(a: A) = ""
    }
}

// TESTCASE NUMBER: 2
fun case2(a: A){
    Case2(a)
    Case2(a = a)
}

class Case2 {
    companion object {
        operator fun invoke(a: A) = ""
    }
}

// TESTCASE NUMBER: 3
fun case3(a: A){
    Case3.Companion(a)  //ok resolved to (2)
    Case3.Companion(parameterA = a) //ok resolved to (2)
}
class Case3 {
    companion object {
        operator fun invoke(parameterA: A) = "" //(2)
    }
}
