interface A
interface B : A

interface GenericFooOut<out T>

fun GenericFooOut<A>.ex<caret>t() {}

class SomeClassOut: GenericFooOut<B> {}

fun usage(x: SomeClassOut) {
    <expr>x</expr>
}

