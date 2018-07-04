// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

class Outer {
    inner class Inner @JvmOverloads constructor(val s1: String, val s2: String = "OK") {

    }
}

fun box(): String {
    val outer = Outer()
    val c = (Outer.Inner::class.java.getConstructor(Outer::class.java, String::class.java).newInstance(outer, "shazam"))
    return c.s2
}
