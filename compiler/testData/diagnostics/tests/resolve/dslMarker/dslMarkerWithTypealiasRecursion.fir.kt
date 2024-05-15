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
fun Foo.zbar(body: ZBar.() -> Unit) = Bar().body()

fun test() {
    Foo().foo {
        zbar <!RECURSIVE_TYPEALIAS_EXPANSION!>{
            foo {}
        }<!>
    }
}
