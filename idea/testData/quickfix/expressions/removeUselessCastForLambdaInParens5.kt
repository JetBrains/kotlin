// "Remove useless cast" "true"
open class A

fun test() {
    class B : A()
    ({ "" } as<caret> () -> String)
}
/* IGNORE_FIR */