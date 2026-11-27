// TARGET_BACKEND: NATIVE
// NATIVE_STANDALONE
// IGNORE_NATIVE: optimizationMode=DEBUG
// IGNORE_NATIVE: optimizationMode=NO
//
// NATIVE_STANDALONE keeps the package as written. The default runner would rename it, and
// localHash of that longer name is not 0. localHash("pkg.Iaa7js64q").toInt() == 0.
// Devirtualization runs before GHA, so this interface's itable id is 0.
// Any's vtable is equals, hashCode, toString, so toString is slot 2.
// a and b pad the itable so m, sorted by signature, is also slot 2.
// A vtable sentinel of 0 then puts both calls on one hub. The hub is sized from the first
// call (toString, receiver only). m has one more argument, and connecting it to that hub
// throws ArrayIndexOutOfBoundsException. Same-arity calls still devirtualize from their own
// slots, so they cannot show the collision.
package pkg

interface Iaa7js64q {
    fun a(): String = "a"

    fun b(): String = "b"

    fun m(extra: Int): String = "iface"
}

class Impl : Iaa7js64q {
    override fun m(extra: Int) = "M"

    override fun toString() = "T"
}

fun box(): String {
    val x: Iaa7js64q = Impl()
    val text = x.toString()
    val value = x.m(1)
    if (text != "T") return "fail toString: $text"
    if (value != "M") return "fail m: $value"
    return "OK"
}
