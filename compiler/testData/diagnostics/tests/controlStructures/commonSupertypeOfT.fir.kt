// KT-6774 Cannot find equals() when comparing with null

fun <T: Any> fn(t1: T, t2: T?) {
    val x = if (true) t1 else t2
    x == null
    x?.equals(null)
    x?.hashCode()
    x.toString()
    x!!.hashCode()

    val y = t2 ?: t1
    y == t1
    y.equals(null)
    y.hashCode()
    y.toString()
    y.hashCode()
}

interface Tr {
    fun foo()
}

fun <T: Tr> fn(t1: T, t2: T?) {
    val x = if (true) t1 else t2
    x?.foo()
    x!!.foo()

    val y = t2 ?: t1
    y.foo()
}