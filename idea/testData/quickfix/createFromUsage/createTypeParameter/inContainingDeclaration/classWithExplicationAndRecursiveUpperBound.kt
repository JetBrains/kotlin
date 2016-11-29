// "Create type parameter 'X' in class 'Foo'" "true"
// ERROR: Unresolved reference: _
// ERROR: Unresolved reference: _
// ERROR: Unresolved reference: _
// ERROR: Type mismatch: inferred type is A<I> but A<List<[ERROR : _]>> was expected
class A<T : List<T>>

interface I : List<I>

open class Foo(x: A<<caret>X>)

class Bar : Foo(A())

fun test() {
    Foo(A())
    Foo(A<I>())

    object : Foo(A<I>()) {

    }
}