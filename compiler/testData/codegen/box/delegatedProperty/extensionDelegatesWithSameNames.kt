// IGNORE_BACKEND_FIR: JVM_IR
open class C

object O : C()

object K : C()

class D(val value: String) {
    operator fun getValue(thisRef: C, property: Any): String = value
}

val O.prop by D("O")
val K.prop by D("K")

fun box() = O.prop + K.prop
