// "Remove 'in' modifier" "true"
trait A<T> {}

class B : A<<caret>in Int> {}
