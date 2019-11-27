// IGNORE_BACKEND_FIR: JVM_IR
open class A<T> () {
   fun plus(e: T) = B<T> (e)
}

class B<T> (val e: T) : A<T>() {
   fun add() = B<T> (e)
}

fun box() : String {
    return if(A<String>().plus("239").add().e == "239" ) "OK" else "fail"
}
