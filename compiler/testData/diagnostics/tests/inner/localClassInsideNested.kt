// RUN_PIPELINE_TILL: FRONTEND
class Outer {
    class Nested {
        fun foo() {
            class Local {
                val state = <!INACCESSIBLE_OUTER_CLASS_RECEIVER!>outerState<!>
            }
        }
    }
    
    val outerState = 42
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, integerLiteral, localClass, nestedClass,
propertyDeclaration */
