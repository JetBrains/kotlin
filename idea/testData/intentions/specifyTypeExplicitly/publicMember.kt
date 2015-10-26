// IS_APPLICABLE: false
// WITH_RUNTIME
// ERROR: Type mismatch: inferred type is kotlin.String but kotlin.Unit was expected

class A {
    public fun <caret>foo() {
        return ""
    }
}
