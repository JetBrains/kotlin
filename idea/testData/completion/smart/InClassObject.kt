class C {
    default object {
        fun foo(): C {
            return <caret>
        }
    }
}

// ABSENT: this@C
