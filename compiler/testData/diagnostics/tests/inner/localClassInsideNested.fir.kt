class Outer {
    class Nested {
        fun foo() {
            class Local {
                val state = outerState
            }
        }
    }
    
    val outerState = 42
}