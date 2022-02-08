// LANGUAGE: -ProhibitSimplificationOfNonTrivialConstBooleanExpressions
// IGNORE_BACKEND_FIR: JVM_IR
// FIR status: don't support legacy feature
fun box() : String = when (true) {
    ((true)) -> "OK"
    (1 == 2) -> "Not ok"
}
