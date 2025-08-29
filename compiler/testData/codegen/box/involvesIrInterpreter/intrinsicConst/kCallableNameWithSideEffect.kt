// LANGUAGE: +IntrinsicConstEvaluation
// IGNORE_IR_DESERIALIZATION_TEST: NATIVE
// ^^^ KT-73621: EVALUATED{FIR} is shown instead of EVALUATED
// WITH_STDLIB

fun <T> T.id() = this

fun someSideEffect(value: Any?) = {}

class A {
    val a = ""
    fun b() = ""
    fun withParameters(a: Int, b: String) = ""

    init {
        someSideEffect("A init")
    }

    fun test() {
        val a = A::a.<!EVALUATED{IR}("a")!>name<!>
        val b = A::b.<!EVALUATED{IR}("b")!>name<!>

        val c = ::A.<!EVALUATED{IR}("<init>")!>name<!>
        val d = this::a.<!EVALUATED{IR}("a")!>name<!>

        val e = A()::b.<!EVALUATED{IR}("b")!>name<!>
        val f = getA()::b.<!EVALUATED{IR}("b")!>name<!>

        val temp = A()
        val g = temp::b.<!EVALUATED{IR}("b")!>name<!>
        val insideStringConcat = "${temp::b.<!EVALUATED{IR}("b")!>name<!>}"

        val complexExpression1 = A()::a.<!EVALUATED{IR}("a")!>name<!> + A()::b.<!EVALUATED{IR}("b")!>name<!>
        val complexExpression2 = <!EVALUATED{IR}("ab")!>A::a.name + A::b.name<!>

        val recursive = ::test.<!EVALUATED{IR}("test")!>name<!>

        val wihtParams1 = A::withParameters.<!EVALUATED{IR}("withParameters")!>name<!>
        val wihtParams2 = A()::withParameters.<!EVALUATED{IR}("withParameters")!>name<!>
    }

    fun getA(): A = A()
}

// STOP_EVALUATION_CHECKS
fun box(): String {
    A().test()
    return "OK"
}
