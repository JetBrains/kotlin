class C {
    companion object {
        fun foo(): C {
            return <caret>
        }
    }
}

// ABSENT: this@C
