// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JVM_IR
open class B {
    val p = "OK"
}

class BB : B()

interface Z<T :B > {
    fun T.getString() : String {
        return p
    }

    fun test(s: T) : String {
        return s.extension()
    }

    private fun T.extension(): String {
        return getString()
    }
}

object Z2 : Z<BB> {

}

fun box() : String {
    return Z2.test(BB())
}

