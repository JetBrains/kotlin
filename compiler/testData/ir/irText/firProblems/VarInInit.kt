// FIR_IDENTICAL
class Some(var foo: Int) {
    init {
        if (foo < 0) {
            foo = 0
        }
    }
}