// FIR_IDENTICAL
// SKIP_TXT

val w: Int = 2

class Outer {
    private inner class Inner private constructor(x: Int) {
        constructor() : this(w)
    }
}
