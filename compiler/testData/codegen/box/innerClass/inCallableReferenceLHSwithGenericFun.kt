// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS

class Outer<A> {
    inner class Inner<C> {
        fun <T> id(x: T): T = x
    }

    val refFoo : Inner<Int>.(String)->String = Inner<Int>::id
}

fun box(): String {
    val refBar : Outer<Int>.Inner<Int>.(String)->String = Outer<Int>.Inner<Int>::id
    return Outer<Int>().refFoo(Outer<Int>().Inner(), "O") + refBar(Outer<Int>().Inner(),"K")
}