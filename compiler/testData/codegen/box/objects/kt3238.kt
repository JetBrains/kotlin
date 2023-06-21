// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

object Obj {
    class Inner() {
        fun ok() = "OK"
    }
}

fun box() : String {
    val klass = Obj.Inner::class.java
    val cons = klass.getConstructors()!![0]
    val inner = cons.newInstance(*(arrayOfNulls<String>(0) as Array<String>))
    return "OK"
}
