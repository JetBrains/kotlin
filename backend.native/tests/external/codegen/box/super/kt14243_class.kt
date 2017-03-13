
open class Z<T, Y> {
    open fun test(p: T, z: Y): T {
        return p
    }
}

open class ZImpl<X> : Z<String, X>()

open class ZImpl2 : ZImpl<String>()

class ZImpl3 : ZImpl2() {
    override fun test(p: String, z: String): String {
        return super.test(p, z)
    }
}

fun box(): String {
    return ZImpl3().test("OK", "fail")
}