// TARGET_BACKEND: JVM
// FILE: J.java

interface J {
    String foo(String... a);
}

// FILE: 1.kt

class Inv : J {
    override fun foo(a: Array<String>): String = a[0]
}

class Out : J {
    override fun foo(a: Array<out String>): String = a[0]
}

class Vararg : J {
    override fun foo(vararg a: String): String = a[0]
}

fun box(): String {
    if (Inv().foo(arrayOf("OK")) != "OK") return "Fail Inv"
    if (Out().foo(arrayOf("OK")) != "OK") return "Fail Out"
    if (Vararg().foo("OK") != "OK") return "Fail Vararg"
    return "OK"
}
