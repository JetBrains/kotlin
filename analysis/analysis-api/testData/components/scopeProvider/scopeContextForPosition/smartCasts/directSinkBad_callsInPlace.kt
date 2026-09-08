// WITH_STDLIB

fun test() {
    var x: Int? = 42
    run {
        x = null
    }
    if (x != null)
        <expr>x.inc()</expr>
}