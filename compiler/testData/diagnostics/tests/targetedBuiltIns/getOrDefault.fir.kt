// FULL_JDK

abstract class A : Map<Int, String>

fun foo(x: Map<Int, String>, a: A, b: java.util.HashMap<Int, String>) {
    x.getOrDefault(1, "")
    x.getOrDefault("", "")
    x.<!INAPPLICABLE_CANDIDATE!>getOrDefault<!>(1, 2)
    x.<!INAPPLICABLE_CANDIDATE!>getOrDefault<!>("", 2)

    a.getOrDefault(1, "")
    a.getOrDefault("", "")
    a.<!INAPPLICABLE_CANDIDATE!>getOrDefault<!>(1, 2)
    a.<!INAPPLICABLE_CANDIDATE!>getOrDefault<!>("", 2)

    b.getOrDefault(1, "")
    b.getOrDefault("", "")
    b.<!INAPPLICABLE_CANDIDATE!>getOrDefault<!>(1, 2)
    b.<!INAPPLICABLE_CANDIDATE!>getOrDefault<!>("", 2)
}
