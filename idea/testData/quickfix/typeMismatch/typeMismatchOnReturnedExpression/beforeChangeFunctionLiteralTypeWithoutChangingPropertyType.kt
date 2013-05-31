// "Change function literal return type to 'String'" "true"
fun foo() {
    val f: (String) -> String = {
        (s: Any): Int ->
        ""<caret>
    }
}