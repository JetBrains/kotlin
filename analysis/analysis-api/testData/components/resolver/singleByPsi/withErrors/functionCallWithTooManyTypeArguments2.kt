fun <A> generic(a: A) { }

fun foo() {
    <expr>generic<Int, String>(5)</expr>
}