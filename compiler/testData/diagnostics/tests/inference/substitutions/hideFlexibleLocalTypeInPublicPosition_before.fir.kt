// ISSUE: KT-30054
// LANGUAGE: -KeepNullabilityWhenApproximatingLocalType
// FILE: J.java
public class J {
    public static <T> T flexibleId(T x) { return x; }
}

// FILE: main.kt
interface I {
    fun foo(): String
}

fun bar(condition: Boolean) /*: I! */ =
    J.flexibleId(object : I {
        override fun foo() = "may or may not check for null first"
        fun baz() = "invisible"
    })

fun main() {
    bar(false).<!UNRESOLVED_REFERENCE!>baz<!>()
    bar(false).foo()
    bar(false)?.foo()
}
