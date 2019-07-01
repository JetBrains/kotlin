// "Create type parameter 'X' in function 'foo'" "true"
// ERROR: Unresolved reference: _
// COMPILER_ARGUMENTS: -XXLanguage:-NewInference
class A<T : List<T>>

interface I : List<I>

fun foo(x: A<<caret>X>) {

}

fun test() {
    foo(A())
    foo(A<I>())
}