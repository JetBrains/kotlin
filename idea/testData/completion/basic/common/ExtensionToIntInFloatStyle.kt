val Int.f: Float get() = this.toFloat()

val test = 1.<caret>

// INVOCATION_COUNT: 1
// EXIST: f