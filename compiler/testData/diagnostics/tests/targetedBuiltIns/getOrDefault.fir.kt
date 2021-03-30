// FULL_JDK

abstract class A : Map<Int, String>

fun foo(x: Map<Int, String>, a: A, b: java.util.HashMap<Int, String>) {
    x.getOrDefault(1, "")
    x.getOrDefault(<!ARGUMENT_TYPE_MISMATCH!>""<!>, "")
    x.getOrDefault(1, <!ARGUMENT_TYPE_MISMATCH!>2<!>)
    x.getOrDefault(<!ARGUMENT_TYPE_MISMATCH!>""<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>)

    a.getOrDefault(1, "")
    a.getOrDefault(<!ARGUMENT_TYPE_MISMATCH!>""<!>, "")
    a.getOrDefault(1, <!ARGUMENT_TYPE_MISMATCH!>2<!>)
    a.getOrDefault(<!ARGUMENT_TYPE_MISMATCH!>""<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>)

    b.getOrDefault(1, "")
    b.getOrDefault(<!ARGUMENT_TYPE_MISMATCH!>""<!>, "")
    b.getOrDefault(1, <!ARGUMENT_TYPE_MISMATCH!>2<!>)
    b.getOrDefault(<!ARGUMENT_TYPE_MISMATCH!>""<!>, <!ARGUMENT_TYPE_MISMATCH!>2<!>)
}
