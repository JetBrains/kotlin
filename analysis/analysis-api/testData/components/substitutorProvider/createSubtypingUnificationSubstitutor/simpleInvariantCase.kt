interface A
interface B : A

interface GenericFoo<T>

fun GenericFoo<A>.ext() {
    th<caret_1_right>is
}

class SomeClass: GenericFoo<B> {}

fun usage(xx: SomeClass) {
    x<caret_1_left>x
}
