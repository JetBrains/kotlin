// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS)
annotation class XMarker

@XMarker
class Foo

class Bar

typealias YBar = <!RECURSIVE_TYPEALIAS_EXPANSION!>ZBar<!>
typealias ZBar = <!RECURSIVE_TYPEALIAS_EXPANSION!>YBar<!>

fun Foo.foo(body: Foo.() -> Unit) = body()
fun Foo.zbar(<!OI;UNUSED_PARAMETER!>body<!>: <!RECURSIVE_TYPEALIAS_EXPANSION!>ZBar<!>.() -> Unit) = Bar().<!OI;UNRESOLVED_REFERENCE_WRONG_RECEIVER!>body<!>()

fun test() {
    Foo().foo {
        zbar {
            <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>foo<!> {}
        }
    }
}