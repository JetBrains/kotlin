// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL

class B<O>(val obj: O) {
    val v = B(obj)
}
