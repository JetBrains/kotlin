// RUN_PIPELINE_TILL: FRONTEND
class B

class A {
    operator fun B.invoke() {}
}

val B.a: () -> Int  get() = { 5 }

fun test(a: A, b: B) {
    val x: Int = b.a()

    b.(<!UNRESOLVED_REFERENCE!>a<!>)()

    <!CANNOT_INFER_PARAMETER_TYPE!>with<!>(b) {
        val y: Int = a()
        (<!UNRESOLVED_REFERENCE!>a<!>)()
    }
}
