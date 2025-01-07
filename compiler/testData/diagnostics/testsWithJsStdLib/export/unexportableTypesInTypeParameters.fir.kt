// OPT_IN: kotlin.js.ExperimentalJsExport
// DIAGNOSTICS: -UNUSED_PARAMETER
// RENDER_DIAGNOSTICS_MESSAGES

package foo

abstract class C
interface I

@JsExport
fun <T : <!NON_EXPORTABLE_TYPE("upper bound; C")!>C<!>>foo() { }

@JsExport
class A<T : <!NON_EXPORTABLE_TYPE("upper bound; C")!>C<!>, S: <!NON_EXPORTABLE_TYPE("upper bound; I")!>I<!>>

@JsExport
interface I2<T> where T : <!NON_EXPORTABLE_TYPE("upper bound; C")!>C<!>, T : <!NON_EXPORTABLE_TYPE("upper bound; I")!>I<!>

@JsExport
class B<T>(val a: T, <!NON_EXPORTABLE_TYPE("parameter; Comparable<T (of class B<T>)>")!>val b: Comparable<T><!>) {
    <!NON_EXPORTABLE_TYPE("property; Comparable<T (of class B<T>)>")!>val c: Comparable<T><!> = b
}

@JsExport
class D<T>(val a: T, val b: Array<T>)
