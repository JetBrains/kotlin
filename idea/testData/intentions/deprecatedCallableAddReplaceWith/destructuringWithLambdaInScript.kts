// IS_APPLICABLE: false
// WITH_RUNTIME
// SKIP_ERRORS_BEFORE
// SKIP_ERRORS_AFTER

val (x, y) = run <caret>{

}

fun run(f: () -> Unit) = f()
