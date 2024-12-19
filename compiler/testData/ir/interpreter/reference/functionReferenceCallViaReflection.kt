// IGNORE_BACKEND_K1: ANY

@CompileTimeCalculation
class A(val a: Int) {
    fun foo(): Int {
        return a
    }
}

const val functionCall = <!EVALUATED: `1`!>A::foo.call(A(1))<!>

const val functionWithReceiverCall = <!EVALUATED: `2`!>A(2)::foo.call()<!>
