fun foo(vararg s: String){}

fun bar() {
    foo(""<caret>)
}