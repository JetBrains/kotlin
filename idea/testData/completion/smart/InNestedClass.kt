class Outer {
    class Nested {
        fun foo(): Outer {
            return <caret>
        }
    }
}

// ABSENT: this@Outer
