// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE
// WITH_RUNTIME

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
