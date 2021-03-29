// "Remove useless cast" "true"
class A {
    fun foo() {}
}

fun test() {
    A().foo()
    ({ "" } as<caret> () -> String)
}
/* IGNORE_FIR */