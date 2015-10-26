// "Create property 'foo' as constructor parameter" "true"
// ERROR: No value passed for parameter foo
// ERROR: No value passed for parameter foo
// ERROR: No value passed for parameter foo

enum class E {
    A,
    B {
        val t: Int = <caret>foo
    },
    C
}