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

class a<!CONFLICTING_OVERLOADS!>()<!> { }

<!CONFLICTING_OVERLOADS!>@Deprecated("a", level = DeprecationLevel.HIDDEN)
fun a()<!> = 1

class b @Deprecated("b", level = DeprecationLevel.HIDDEN) <!CONFLICTING_OVERLOADS!>constructor()<!> { }

<!CONFLICTING_OVERLOADS!>fun b()<!> = 2

class Tram {
    <!CONFLICTING_OVERLOADS!>@Deprecated("f", level = DeprecationLevel.HIDDEN)
    fun f()<!> { }

    class f<!CONFLICTING_OVERLOADS!>()<!> { }
}

class Yvayva {
    companion object {
        <!CONFLICTING_OVERLOADS!>@Deprecated("fghj", level = DeprecationLevel.HIDDEN)
        fun fghj()<!> { }

        class fghj<!CONFLICTING_OVERLOADS!>()<!> { }
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
