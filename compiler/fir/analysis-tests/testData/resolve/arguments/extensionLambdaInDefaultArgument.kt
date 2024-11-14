// RUN_PIPELINE_TILL: BACKEND
fun test(
    f: String.() -> Int = { length }
): Int {
    return "".f()
}
