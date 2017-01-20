class A(val s: String = "FAIL")

private fun foo(a: A?, aOther: A?): A {
    return if (a == null) {
        A()
    }
    else {
        if (aOther == null) {
            return A()
        }
        aOther
    }
}

fun box() = foo(A("???"), A("OK")).s
