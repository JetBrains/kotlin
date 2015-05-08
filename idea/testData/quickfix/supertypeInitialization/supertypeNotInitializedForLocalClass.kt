// "Change to constructor invocation" "true"
fun bar() {
    abstract class Foo {}
    class A : <caret>Foo {}
}