public open class A() {
    public open val foo: Int? = 1
}

infix fun Int.bar(i: Int) = i

fun test() {
    val p = A()
    // For open value properties, smart casts should not work
    if (p.foo is Int) <!SMARTCAST_IMPOSSIBLE!>p.foo<!> bar 11
}
