// IGNORE_REVERSED_RESOLVE
// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_VARIABLE

open class Super<T> {
    inner open class Inner {
        open fun getOuter(): Super<T> = throw UnsupportedOperationException()
    }
}

class Sub<T1>(): Super<T1>() {
    inner class SubInner : Super<T1>.Inner() { // 'Inner' is unresolved
        // Also, T1 is not resolved to anything, and not marked as resolved
        init {
            val x: Super<T1>.Inner = this // T1 is not resolved to anything
        }

        override fun getOuter(): Sub<T1> = throw UnsupportedOperationException()
    }
}
