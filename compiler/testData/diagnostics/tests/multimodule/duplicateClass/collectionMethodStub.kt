// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL
// SKIP_TXT
// FILE: f1.kt

package kotlin

class Unit

// FILE: f2.kt

class C: MutableIterator<Int> {
    override fun remove(): Unit {
        throw UnsupportedOperationException()
    }
    override fun next(): Int {
        throw UnsupportedOperationException()
    }
    override fun hasNext(): Boolean {
        throw UnsupportedOperationException()
    }

}
