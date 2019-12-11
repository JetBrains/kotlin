class MyClass(var p: Any)

fun bar(s: Any): Int {
    return s.hashCode()
}

fun foo(m: MyClass): Int {
    m.p = "xyz"
    return bar(m.p)
}