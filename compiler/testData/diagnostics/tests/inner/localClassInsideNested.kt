class Outer {
    class Nested {
        fun foo() {
            class Local {
                val state = <!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>outerState<!>
            }
        }
    }
    
    val outerState = 42
}
