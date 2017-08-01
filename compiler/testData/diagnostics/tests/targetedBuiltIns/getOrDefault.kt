// FULL_JDK

abstract class A : Map<Int, String>

fun foo(x: Map<Int, String>, a: A, b: java.util.HashMap<Int, String>) {
    x.getOrDefault(1, "")
    x.getOrDefault(<!TYPE_MISMATCH!>""<!>, "")
    x.getOrDefault(1, <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2<!>)
    x.getOrDefault(<!TYPE_MISMATCH!>""<!>, <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2<!>)

    a.getOrDefault(1, "")
    a.getOrDefault(<!TYPE_MISMATCH!>""<!>, "")
    a.getOrDefault(1, <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2<!>)
    a.getOrDefault(<!TYPE_MISMATCH!>""<!>, <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2<!>)

    b.getOrDefault(1, "")
    b.getOrDefault(<!TYPE_MISMATCH!>""<!>, "")
    b.getOrDefault(1, <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2<!>)
    b.getOrDefault(<!TYPE_MISMATCH!>""<!>, <!CONSTANT_EXPECTED_TYPE_MISMATCH!>2<!>)
}
