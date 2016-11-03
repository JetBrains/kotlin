// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

object Obj {
    class Inner() {
        fun ok() = "OK"
    }
}

fun box() : String {
    val klass = Class.forName("Obj\$Inner")!!
    val cons = klass.getConstructors()!![0]
    val inner = cons.newInstance(*(arrayOfNulls<String>(0) as Array<String>))
    return "OK"
}
