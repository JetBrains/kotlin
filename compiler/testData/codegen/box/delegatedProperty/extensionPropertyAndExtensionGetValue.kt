// IGNORE_BACKEND_FIR: JVM_IR
class A(val o: String)

interface I {
    val k: String
}

inline operator fun A.getValue(thisRef: I, property: Any): String = o + thisRef.k

class B(override val k: String) : I

val B.prop by A("O")

fun box() = B("K").prop