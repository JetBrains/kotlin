// constructor vs. fun overload

// FILE: J.java
package constructorVsFun;

public class J {
    public J(String s) {}
}

// FILE: test.kt
package constructorVsFun

class a<!CONFLICTING_OVERLOADS!>()<!> { }

<!CONFLICTING_OVERLOADS!>fun a()<!> = 1

class Tram {
    <!CONFLICTING_OVERLOADS!>fun f()<!> { }

    class f<!CONFLICTING_OVERLOADS!>()<!> { }
}

class Yvayva {
    companion object {
        <!CONFLICTING_OVERLOADS!>fun fghj()<!> { }

        class fghj<!CONFLICTING_OVERLOADS!>()<!> { }
    }
}

class Rtyu {
    fun ololo() { }

    companion object {
        class ololo() { }
    }
}

<!CONFLICTING_OVERLOADS!>fun J(s: String)<!> { }
