// !WITH_NEW_INFERENCE
class MyClass(var p: String?)

fun bar(s: String): Int {
    return s.length
}

fun foo(m: MyClass): Int {
    m.p = "xyz"
    return bar(<!NI;SMARTCAST_IMPOSSIBLE, SMARTCAST_IMPOSSIBLE!>m.p<!>)
}