// WITH_STDLIB
class C(val x: String)

class D(c: C) {
    val x by c::x
}

fun box(): String = D(C("OK")).x
