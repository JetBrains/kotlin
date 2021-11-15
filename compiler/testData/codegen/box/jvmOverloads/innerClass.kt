// TARGET_BACKEND: JVM

// WITH_STDLIB

class Outer {
    inner class Inner @JvmOverloads constructor(val s1: String, val s2: String = "OK") {

    }
}

fun box(): String {
    val outer = Outer()
    val c = (Outer.Inner::class.java.getConstructor(Outer::class.java, String::class.java).newInstance(outer, "shazam"))
    return c.s2
}
