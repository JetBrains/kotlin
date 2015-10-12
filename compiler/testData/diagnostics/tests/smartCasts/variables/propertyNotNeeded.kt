class MyClass(var p: String?)

fun bar(s: String?): Int {
    return s?.length ?: -1
}

fun foo(m: MyClass): Int {
    m.p = "xyz"
    return bar(m.p)
}