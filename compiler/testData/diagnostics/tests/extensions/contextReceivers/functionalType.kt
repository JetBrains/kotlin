class Param
class C {
    val c = 42
}
class R {
    val r = 42
}

context(C)
fun R.f1(g: context(C) R.(Param) -> Unit) {
    g(this@C, this@R, Param())
}

context(C)
fun f2(g: context(C) (Param) -> Unit) {
    g(this@C, Param())
}

context(C)
fun R.f3(g: context(C) R.() -> Unit) {
    g(this@C, this@R)
}

context(C)
fun f4(g: context(C) () -> Unit) {
    g(this@C)
}

fun test() {
    val lf1: context(C) R.(Param) -> Unit = { _ ->
        r
        c
    }
    val lf2: context(C) (Param) -> Unit = { _ ->
        c
    }
    val lf3: context(C) R.() -> Unit = {
        r
        c
    }
    val lf4: context(C) () -> Unit = {
        c
    }

    with(C()) {
        with(R()) {
            f1(lf1)
            f1 { _ ->
                r
                c
            }

            f2(lf2)
            f2 { _ ->
                c
            }

            f3(lf3)
            f3 {
                r
                c
            }

            f4(lf4)
            f4 {
                c
            }
        }
    }
}