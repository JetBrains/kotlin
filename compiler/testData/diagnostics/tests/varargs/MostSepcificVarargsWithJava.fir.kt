// FILE: kotlin.kt
fun main(j : C, s : Array<String?>) {
    j.from()
    j.from("")
    j.from("", "")
    j.<!AMBIGUITY!>from<!>("", "", "")

    j.<!AMBIGUITY!>from<!>("", *s) // This should not be an ambiguity, see KT-1842
    j.from(*s)
}

// FILE: C.java
public class C {
    void from() {}
    void from(String s) {}
    void from(String s, String s1) {}
    void from(String... s) {}
    void from(String s1, String... s) {}
}