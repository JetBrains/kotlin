class Outer {
    class Nested {
        fun foo() {
            class Local {
                val state = <!UNRESOLVED_REFERENCE!>outerState<!>
            }
        }
    }
    
    val outerState = 42
}