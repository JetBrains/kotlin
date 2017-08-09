@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS)
annotation class XMarker

@XMarker
class Foo

class Bar

@XMarker
typealias XBar = Bar

typealias XXBar = XBar

fun Foo.foo(body: Foo.() -> Unit) = body()
fun Foo.xbar(body: XBar.() -> Unit) = Bar().body()
fun Foo.xxbar(body: XXBar.() -> Unit) = Bar().body()

fun test() {
    Foo().foo {
        xbar {
            <!DSL_SCOPE_VIOLATION!>foo<!> {}
        }
        xxbar {
            <!DSL_SCOPE_VIOLATION!>foo<!> {}
        }
    }
}