@CompileTimeCalculation
class A(val a: Int) {
    fun foo(): Int {
        return a
    }

    suspend fun baz(): Int {
        return a
    }
}

const val functionName = <!EVALUATED: `foo`!>A::foo.name<!>
const val functionInvoke = <!EVALUATED: `1`!>A::foo.invoke(A(1))<!>

const val functionWithReceiverName = <!EVALUATED: `foo`!>A(2)::foo.name<!>
const val functionWithReceiverInvoke = <!EVALUATED: `2`!>A(2)::foo.invoke()<!>

// THIS IS WRONG, suspend fun must be called from coroutine ot another suspend function.
// It is used here just for test purposes.
const val suspendFunctionInvoke = <!EVALUATED: `3`!>A::baz.invoke(A(3))<!>
