// "Create function 'foo' from usage" "true"
// ERROR: Unresolved reference: s

class A<T>(val n: T)

fun test(): Int {
    return A(1).<caret>foo(s, 1)
}