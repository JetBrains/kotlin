abstract class A<T>

typealias AS = A<String>

class C : AS {
    <caret>constructor()
}