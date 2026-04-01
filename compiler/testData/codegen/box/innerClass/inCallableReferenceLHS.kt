// LANGUAGE: +ProperSupportOfInnerClassesInCallableReferenceLHS

open class A<X: String>(val x: X) {
    inner class B<Y> {
        fun foo(): String = x
        fun bar(): X = "K" as X
    }

    val refFoo = B<Int>::foo
}

class C: A<String>("") {
    val refBar = B<Int>::bar
}

fun box(): String {
    return A<String>("O").run {
        refFoo(B())
    } + C().run {
        refBar(B())
    }
}
