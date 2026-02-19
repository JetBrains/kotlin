fun <A, B, C> generic(a: A, b: B, c: C) { }

fun foo() {
    <expr>generic<String, String>("a", "b", "c")</expr>
}