// PARAM_DESCRIPTOR: value-parameter n: kotlin.Int defined in E.A.foo
// PARAM_TYPES: kotlin.Int
enum class E {
    // SIBLING:
    A {
        fun foo(n: Int) = <selection>n + 1</selection>
    },
    B,
    C
}