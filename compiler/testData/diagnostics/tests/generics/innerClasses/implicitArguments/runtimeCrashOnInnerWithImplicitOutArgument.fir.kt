// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-71579

class Out<out T>(private var p: T) {
    fun getP(): T = p

    inner class Inner {
        fun getP(): T {
            return p
        }
    }

    fun accept(inner: <!TYPE_VARIANCE_CONFLICT_ERROR!>Inner<!>) {
        p = inner.getP()
    }
}

fun box(): String {
    val outAny1: Out<Any> = Out<Any>("1")
    val outInt: Out<Int> = Out(1)

    val outAny2: Out<Any> = outInt

    outAny2.accept(outAny1.Inner())

    outInt.getP().plus(1) // ClassCastException: java.lang.String cannot be cast to class java.lang.Number

    return "OK"
}
