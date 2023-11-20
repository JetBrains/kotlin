// !OPT_IN: kotlin.js.ExperimentalJsExport
// !DIAGNOSTICS: -UNUSED_PARAMETER
// !RENDER_DIAGNOSTICS_MESSAGES

package foo

abstract class C
interface I

@JsExport
fun <T : <!NON_EXPORTABLE_TYPE("upper bound; foo.C")!>C<!>>foo() { }

@JsExport
class A<T : <!NON_EXPORTABLE_TYPE("upper bound; foo.C")!>C<!>, S: <!NON_EXPORTABLE_TYPE("upper bound; foo.I")!>I<!>>

@JsExport
interface I2<T> where T : <!NON_EXPORTABLE_TYPE("upper bound; foo.C")!>C<!>, T : <!NON_EXPORTABLE_TYPE("upper bound; foo.I")!>I<!>

@JsExport
class B<T>(val a: T, <!NON_EXPORTABLE_TYPE("parameter; kotlin.Comparable<T>")!>val b: Comparable<T><!>) {
    <!NON_EXPORTABLE_TYPE("property; kotlin.Comparable<T>")!>val c: Comparable<T><!> = b
}

@JsExport
class D<T>(val a: T, val b: Array<T>)
