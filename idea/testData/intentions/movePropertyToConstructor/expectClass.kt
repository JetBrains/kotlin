// IS_APPLICABLE: false
// ERROR: The feature "multi platform projects" is experimental and should be enabled explicitly

expect class Outer {
    class Nested {
        val <caret>x: Int
    }
}