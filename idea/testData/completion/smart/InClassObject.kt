class C {
    class object {
        fun foo(): C {
            return <caret>
        }
    }
}

// ABSENT: this@C
