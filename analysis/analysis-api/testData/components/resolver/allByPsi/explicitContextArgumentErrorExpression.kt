context(s: String)
fun foo() {}

fun bar() {
    foo(s = )
}
// LANGUAGE: +ContextParameters +ExplicitContextArguments