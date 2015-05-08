// "Change function literal return type to 'String'" "true"
fun foo(f: (String) -> Any) {
    foo {
        (s: String): Int ->
        s<caret>
    }
}