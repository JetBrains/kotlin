interface A<T>

class B: A<Int>

fun A<in Nothing>.f<caret>oo() {}

fun usage(x: B) {
    <expr>x</expr>
}