// FIR_COMPARISON
inline fun <T> T.apply(block: T.() -> Unit): T { block(); return this }

val v = JavaClass().apply {
    foo = X()
    foo.<caret>
}

// EXIST: f
