// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR

inline class Composed(val s: String) {
    private constructor(s1: String, s2: String) : this(s1 + s2)

    companion object {
        fun p1(s: String) = Composed("O", s)
    }
}

fun box() = Composed.p1("K").s