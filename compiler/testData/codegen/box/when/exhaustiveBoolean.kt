// IGNORE_BACKEND_FIR: JVM_IR
fun box() : String = when (true) {
    ((true)) -> "OK"
    (1 == 2) -> "Not ok"
}
