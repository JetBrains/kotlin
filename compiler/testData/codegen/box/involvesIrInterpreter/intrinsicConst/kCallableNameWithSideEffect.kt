// !LANGUAGE: +IntrinsicConstEvaluation
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

fun <T> T.id() = this

class A {
    val a = ""
    fun b() = ""

    init {
        println("A init")
    }

    fun test() {
        val a = A::a.<!EVALUATED("a")!>name<!>
        val b = A::b.<!EVALUATED("b")!>name<!>

        val c = ::A.<!EVALUATED("<init>")!>name<!>
        val d = this::a.name

        val e = A()::b.name
        val f = getA()::b.name

        val temp = A()
        val g = temp::b.name
    }

    fun getA(): A = A()
}

// STOP_EVALUATION_CHECKS
fun box(): String {
    A().test()
    return "OK"
}
