annotation class Anno(val value: String)

fun test() {
    // Annotation class cannot be instantiated
    consume(<expr>::Anno</expr>)
}

fun consume(f: (String) -> Any) {}
// COMPILATION_ERRORS