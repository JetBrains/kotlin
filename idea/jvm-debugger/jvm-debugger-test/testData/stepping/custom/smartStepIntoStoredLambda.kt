package smartStepIntoStoredLambda

fun foo(a: Any) {}

fun store(a: () -> Unit): () -> Unit {
    return a
}

fun main(args: Array<String>) {
    // SMART_STEP_INTO_BY_INDEX: 2
    //Breakpoint!
    val some = store() {
        foo("hi")
    }

    some()
}