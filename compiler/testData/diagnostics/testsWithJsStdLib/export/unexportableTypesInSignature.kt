// !OPT_IN: kotlin.js.ExperimentalJsExport
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !RENDER_DIAGNOSTICS_MESSAGES

package foo

class C

@JsExport
fun foo(<!NON_EXPORTABLE_TYPE("parameter; C")!>x: C<!>) {
}

<!NON_EXPORTABLE_TYPE("return; C")!>@JsExport
fun bar()<!> = C()

<!NON_EXPORTABLE_TYPE("property; C")!>@JsExport
val x: C<!> = C()

<!NON_EXPORTABLE_TYPE("property; C")!>@JsExport
var x2: C<!>
    get() = C()
    set(value) { }

@JsExport
class A(
    <!NON_EXPORTABLE_TYPE("parameter; C")!>val x: C<!>,
    <!NON_EXPORTABLE_TYPE("parameter; C")!>y: C<!>
) {
    <!NON_EXPORTABLE_TYPE("return; C")!>fun foo(<!NON_EXPORTABLE_TYPE("parameter; C")!>x: C<!>)<!> = x

    <!NON_EXPORTABLE_TYPE("property; C")!>val x2: C<!> = C()

    <!NON_EXPORTABLE_TYPE("property; C")!>var x3: C<!>
        get() = C()
        set(value) { }
}

@JsExport
fun foo2() {
}

@JsExport
fun foo3(<!NON_EXPORTABLE_TYPE("parameter; Unit")!>x: Unit<!>) {
}

@JsExport
fun foo4(x: () -> Unit) {
}

@JsExport
fun foo5(<!NON_EXPORTABLE_TYPE("parameter; (Unit) -> Unit")!>x: (Unit) -> Unit<!>) {
}

@JsExport
fun foo6(x: (A) -> A) {
}

@JsExport
fun foo7(x: List<Int>) {
}

@JsExport
fun foo8(x: MutableList<Int>) {
}

@JsExport
fun foo9(x: Set<Int>) {
}

@JsExport
fun foo10(x: MutableSet<Int>) {
}

@JsExport
fun foo11(x: Map<String, Int>) {
}

@JsExport
fun foo12(x: MutableMap<String, Int>) {
}
