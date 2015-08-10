interface X

open class Y : X {
    open inner class Inner {
        fun Any.foo(): X {
            if (this@Inner is X) return this@<caret>
        }
    }
}

// ORDER: this@Inner
// ORDER: this@Y
// ORDER: this@foo
