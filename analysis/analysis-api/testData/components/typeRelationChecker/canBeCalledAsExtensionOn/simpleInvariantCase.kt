interface A
interface B : A

interface GenericFoo<T>

fun GenericFoo<A>.ex<caret>t() {}

class SomeClass: GenericFoo<B> {}

fun usage(x: SomeClass) {
    <expr>x</expr>
}

