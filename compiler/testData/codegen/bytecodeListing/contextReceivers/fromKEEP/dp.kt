// LANGUAGE: +ContextReceivers, -ContextParameters
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K2: ANY

class View {
    val coefficient = 42
}

context(View) val Int.dp get() = coefficient * this
