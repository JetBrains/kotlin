// PSI_ELEMENT: org.jetbrains.jet.lang.psi.JetNamedFunction
// OPTIONS: usages

class A<T: Any> {
    public fun <caret>iterator(): Iterator<T> = throw IllegalStateException("")
}

class B<T: Any> {
    public fun iterator(): Iterator<T> = throw IllegalStateException("")
}

fun test() {
    for (a in A<String>()) {}
    for (b in B<String>()) {}
    for (a in A<Int>()) {}
    for (b in B<Int>()) {}
}