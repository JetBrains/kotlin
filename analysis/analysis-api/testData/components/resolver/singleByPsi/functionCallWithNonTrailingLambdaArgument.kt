fun function(a: Int, b: (String) -> Boolean) {}

fun call() {
    <expr>function(1, { s -> true })</expr>
}
