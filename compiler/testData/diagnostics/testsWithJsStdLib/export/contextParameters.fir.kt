// OPT_IN: kotlin.js.ExperimentalJsExport
// LANGUAGE: +ContextParameters
@JsExport
class Scope1 {
    fun foo() {}
}

class Scope2 {
    fun bar() {}
}

@JsExport
context(scope1: Scope1, scope2: Scope2)
fun test1() {
    scope1.foo()
    scope2.bar()
}

@JsExport
fun <A, B, R> context1(a: A, b: B, block: context(A, B) () -> R): R = block(a, b)

@JsExport
fun test2(scope1: Scope1, <!NON_EXPORTABLE_TYPE!>scope2: Scope2<!>){
    context1(scope1, scope2) {
        test1()
    }
}

@JsExport
class C {
    context(scope1: Scope1)
    @JsExport.Ignore
    fun test() {}
}
