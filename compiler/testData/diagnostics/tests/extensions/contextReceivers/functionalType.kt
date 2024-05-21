// FIR_IDENTICAL
// LANGUAGE: +ContextReceivers

class Param
class C {
    val c = 42
}
class R {
    val r = 42
}

@Target(AnnotationTarget.TYPE)
annotation class MyAnnotation

context(C)
fun R.f1(g: context(C) R.(Param) -> Unit) {
    g(this@C, this@R, Param())
}

context(C)
fun R.f2(g: @MyAnnotation context(C) R.(Param) -> Unit) {
    g(this@C, this@R, Param())
}

context(C)
fun f3(g: context(C) (Param) -> Unit) {
    g(this@C, Param())
}

context(C)
fun R.f4(g: context(C) R.() -> Unit) {
    g(this@C, this@R)
}

context(C)
fun f5(g: context(C) () -> Unit) {
    g(this@C)
}

context(C)
fun f6(g: (context(C) () -> Unit)?) {
    g?.invoke(this@C)
}

fun test() {
    val lf1: context(C) R.(Param) -> Unit = { _ ->
        r
        c
    }
    val lf2: @MyAnnotation context(C) R.(Param) -> Unit = { _ ->
        r
        c
    }
    val lf3: context(C) (Param) -> Unit = { _ ->
        c
    }
    val lf4: context(C) R.() -> Unit = {
        r
        c
    }
    val lf5: context(C) () -> Unit = {
        c
    }
    val lf6: (context(C) () -> Unit)? = {
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
                r
                c
            }

            f3(lf3)
            f3 { _ ->
                c
            }

            f4(lf4)
            f4 {
                r
                c
            }

            f5(lf5)
            f5 {
                c
            }

            f6(lf6)
            f6 {
                c
            }
        }
    }
}