class A(val res: Int) {
    fun foo() = res
}

fun A.bar(x: Int) = foo() * x;

object Foo {
    val qqq = 6
}

fun box(): String {
//    var a = "kotlin".length
//    print(a)
//    val g = A::foo
//    print(g(A(78)))
//
//    val f = (if (1 < 2) A(6) else { print(1); A(2)})::foo
//    if (f() != 6) return "fail"
//
//    val z = A(3)::bar
//    if (z(11) != 33) return "fail"
//
//    val q = 100
//    fun A.zzz(x: Int) = (x + q) * foo()
//    val w = A(5)::zzz
//
//    if (w(11) != 555) return "fail"

    val ddd = Foo::qqq
    if (ddd() != 6) return "fail"

    return "OK"
}
