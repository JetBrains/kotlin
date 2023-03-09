// !LANGUAGE: +ContextReceivers
// TARGET_BACKEND: JVM_IR

class View {
    val coefficient = 42
}

context(View) val Int.dp get() = coefficient * this
