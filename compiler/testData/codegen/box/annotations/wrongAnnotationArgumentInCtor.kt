// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// IGNORE_BACKEND: JVM, JS, NATIVE

// Here we check that there is compilation error, so ignore_backend directive is actual

@Target(AnnotationTarget.FIELD, AnnotationTarget.CLASS)
annotation class Anno

class UnresolvedArgument(@Anno(BLA) val s: Int)

class WithoutArguments(@Deprecated val s: Int)

fun box(): String {
    UnresolvedArgument(3)
    WithoutArguments(0)

    return "OK"
}
