interface A
interface B : A

interface GenericFooOut<out T>

fun GenericFooOut<A>.ext() {
    th<caret_1_target>is
}

class SomeClassOut: GenericFooOut<B> {}

fun usage(xx: SomeClassOut) {
    x<caret_1_base>x
}
