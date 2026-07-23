// WITH_STDLIB

class A {
    val x = "unreachable"
}

val bad: Any = "not an A"

class C {
    val y by (bad as A)::x
}

fun box(): String {
    val c = try {
        C()
    } catch (e: ClassCastException) {
        return "OK"
    }
    return "FAIL: no CCE when constructing C, y = ${c.y}"
}
