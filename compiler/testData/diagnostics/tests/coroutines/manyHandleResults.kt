// !DIAGNOSTICS: -UNUSED_PARAMETER
interface A {
    operator fun handleResult(x: Int, y: Continuation<Nothing>) {}
}

interface B {
    operator fun handleResult(x: String, y: Continuation<Nothing>) {}
}

class C : A, B {
    // multiple handleResults
}

fun builder1(coroutine c: A.() -> Continuation<Unit>) {}
fun builder2(coroutine c: B.() -> Continuation<Unit>) {}
fun builder3(<!INAPPLICABLE_MODIFIER!>coroutine<!> c: C.() -> Continuation<Unit>) {}
