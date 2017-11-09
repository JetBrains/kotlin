package test

class C

var C?.p: Int
    get() = 42
    set(value) {}

operator fun Int?.inc(): Int? = this?.inc()

operator fun Int?.get(index: Int): Int = 42
operator fun Int?.set(index: Int, value: Int) {}

fun testProperty(nc: C?) {
    nc?.p++
}

fun testArrayAccess(nc: C?) {
    nc?.p[0]++
}