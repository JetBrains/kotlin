// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JVM

class View {
    val coefficient = 42
}

context(View) val Int.dp get() = coefficient * this