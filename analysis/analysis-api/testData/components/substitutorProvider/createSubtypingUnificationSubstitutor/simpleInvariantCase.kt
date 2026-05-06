interface A
interface B : A

interface GenericFoo<T>

fun GenericFoo<A>.ext() {
    th<caret_1_target>is
}

class SomeClass: GenericFoo<B> {}

fun usage(xx: SomeClass) {
    x<caret_1_base>x
}
