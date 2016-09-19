class C(var x: Int)

fun test(nc: C?) {
    nc?.x = 42
}