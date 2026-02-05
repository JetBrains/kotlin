// LANGUAGE: +IntrinsicConstEvaluation
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
        val a = A::a.name
        val b = A::b.name

        val c = ::A.name
        val d = this::a.name

        val e = A()::b.name
        val f = getA()::b.name

        val temp = A()
        val g = temp::b.name
        val insideStringConcat = "${temp::b.name}"

        val complexExpression1 = A()::a.name + A()::b.name
        val complexExpression2 = A::a.name + A::b.name

        val recursive = ::test.name

        val wihtParams1 = A::withParameters.name
        val wihtParams2 = A()::withParameters.name
    }

    fun getA(): A = A()
}

// STOP_EVALUATION_CHECKS
fun box(): String {
    A().test()
    return "OK"
}
