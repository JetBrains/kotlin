// NI_EXPECTED_FILE
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPEALIAS)
annotation class XMarker

@XMarker
class Foo

class Bar

typealias YBar = <!OTHER_ERROR!>ZBar<!>
typealias ZBar = YBar

fun Foo.foo(body: Foo.() -> Unit) = body()
fun Foo.zbar(body: ZBar.() -> Unit) = Bar().body()

fun test() {
    Foo().foo {
        zbar {
            foo {}
        }
    }
}
