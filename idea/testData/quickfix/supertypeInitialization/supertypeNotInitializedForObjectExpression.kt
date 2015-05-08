// "Change to constructor invocation" "true"
fun bar() {
    abstract class Foo {}
    val foo: Foo = object : <caret>Foo {}
}