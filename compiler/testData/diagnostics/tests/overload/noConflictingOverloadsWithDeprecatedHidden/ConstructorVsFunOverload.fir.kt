// constructor vs. fun overload

// FILE: J.java
package constructorVsFun;

public class J {
    public J(String s) {}
}

// FILE: G.java
package constructorVsFun;

public class G {
    @kotlin.Deprecated(message = "G", level = kotlin.DeprecationLevel.HIDDEN)
    public G(String s) {}
}

// FILE: test.kt
package constructorVsFun

class a() { }

@Deprecated("a", level = DeprecationLevel.HIDDEN)
fun a() = 1

class b @Deprecated("b", level = DeprecationLevel.HIDDEN) constructor() { }

fun b() = 2

class Tram {
    @Deprecated("f", level = DeprecationLevel.HIDDEN)
    fun f() { }

    class f() { }
}

class Yvayva {
    companion object {
        @Deprecated("fghj", level = DeprecationLevel.HIDDEN)
        fun fghj() { }

        class fghj() { }
    }
}

class Rtyu {
    fun ololo() { }

    companion object {
        class ololo() { }
    }
}

@Deprecated("J", level = DeprecationLevel.HIDDEN)
fun J(s: String) { }

fun G(s: String) { }
