// "Remove 'in' modifier" "true"
class A<T> {}

class B {
    var foo = A<<caret>in Int>()
}
