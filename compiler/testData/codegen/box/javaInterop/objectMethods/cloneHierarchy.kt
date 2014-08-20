import java.util.ArrayList

open class A : Cloneable {
    public override fun clone(): A = super.clone() as A
}

open class B(var s: String) : A() {
    override fun clone(): B = super.clone() as B
}

open class C(s: String, var l: ArrayList<Any>): B(s) {
    override fun clone(): C {
        val result = super.clone() as C
        result.l = l.clone() as ArrayList<Any>
        return result
    }
}

fun box(): String {
    val l = ArrayList<Any>()
    l.add(true)

    val c = C("OK", l)
    val d = c.clone()

    if (c.s != d.s) return "Fail s: ${d.s}"
    if (c.l != d.l) return "Fail l: ${d.l}"
    if (c.l identityEquals d.l) return "Fail list identity"
    if (c identityEquals d) return "Fail identity"

    return "OK"
}
