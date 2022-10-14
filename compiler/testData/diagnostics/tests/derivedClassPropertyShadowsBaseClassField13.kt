// FIR_IDENTICAL
// !LANGUAGE: -PreferJavaFieldOverload
// WITH_STDLIB
// FILE: Base.java

public class Base {
    public String a = "a";

    public String b = "b";

    public String c = "c";

    public String d = "d";

    public String e = "e";
}

// FILE: test.kt

class Derived : Base() {
    val a = "aa"

    val b get() = "bb"

    lateinit var c: String

    val d by lazy { "dd" }

    var e: String = "ee"
        set(value) {
            println(value)
            field = value
        }
}

fun test(d: Derived) {
    d.<!OVERLOAD_RESOLUTION_AMBIGUITY!>a<!>
    d.<!OVERLOAD_RESOLUTION_AMBIGUITY!>b<!>
    d.<!OVERLOAD_RESOLUTION_AMBIGUITY!>c<!>
    d.<!OVERLOAD_RESOLUTION_AMBIGUITY!>d<!>
    d.<!OVERLOAD_RESOLUTION_AMBIGUITY!>e<!> = ""
}
