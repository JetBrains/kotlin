fun <T1, O1> function(a: O1, b: (Int) -> Boolean, c: T1) {}
fun <T3, O3> function(a: O3, b: String, c: T3) {}

fun call() {
    <expr>function(1, { s -> true }, "str")</expr>
}
