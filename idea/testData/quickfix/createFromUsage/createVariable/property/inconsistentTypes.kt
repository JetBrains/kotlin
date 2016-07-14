// "Create member property 'A.foo'" "true"
// ERROR: Type mismatch: inferred type is A<Int> but Int was expected
// ERROR: Property must be initialized or be abstract

class A<T>(val n: T)

fun test(): Int {
    return A(1).<caret>foo as A<Int>
}
