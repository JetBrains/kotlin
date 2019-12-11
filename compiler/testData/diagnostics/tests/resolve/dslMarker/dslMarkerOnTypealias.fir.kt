@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS)
annotation class XMarker

@XMarker
class Foo

class Bar

@XMarker
typealias XBar = Bar

typealias XXBar = XBar

fun Foo.foo(body: Foo.() -> Unit) = <!INAPPLICABLE_CANDIDATE!>body<!>()
fun Foo.xbar(body: XBar.() -> Unit) = Bar().<!UNRESOLVED_REFERENCE!>body<!>()
fun Foo.xxbar(body: XXBar.() -> Unit) = Bar().<!UNRESOLVED_REFERENCE!>body<!>()

fun test() {
    Foo().foo {
        <!UNRESOLVED_REFERENCE!>xbar<!> {
            <!UNRESOLVED_REFERENCE!>foo<!> {}
        }
        <!UNRESOLVED_REFERENCE!>xxbar<!> {
            <!UNRESOLVED_REFERENCE!>foo<!> {}
        }
    }
}