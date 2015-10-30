// "Create member function 'foo'" "true"
// ERROR: Type mismatch: inferred type is A<kotlin.Int> but kotlin.Int was expected

class A<T>(val n: T)

fun test(): Int {
    return A(1).<caret>foo("s", 1) as A<Int>
}