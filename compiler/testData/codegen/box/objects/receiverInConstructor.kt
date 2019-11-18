// IGNORE_BACKEND_FIR: JVM_IR
open class A(open val v: String)

fun A.a(newv: String) = object: A("fail") {
   override val v = this@a.v + newv
}

fun box() = A("O").a("K").v
