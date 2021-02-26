// FIR_COMPARISON
package first

class OuterClass {
    class Nested {
        fun foo() {
            out<caret>
        }
    }
}

// INVOCATION_COUNT: 2
// ABSENT: outerExtension