// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// ISSUE: KT-77541

interface Scope {
    fun foo(c: Int)
}

fun box(): String {
    val testContextAnonymousFun: context(Scope, Scope) () -> Unit = context(scope1: Scope, scope2: Scope) fun() {
        scope1.foo(2)
        scope2.foo(3)
    }
    class Local(var x: Int = 0) : Scope {
        override fun foo(c: Int) {
            x += c
        }
    }
    val l = Local()
    context(l) {
        testContextAnonymousFun()
    }
    with(l) {
        testContextAnonymousFun()
    }

    val x: Any = l
    with(x) {
        if (this is Local) {
            testContextAnonymousFun()
        }
    }

    return if (l.x == 15) "OK" else "NOT OK: ${l.x}"
}