interface GenericFooIn<in T>

fun <T: GenericFooIn<T>> GenericFooIn<T>.e<caret>xt() {}

class SomeClassIn: GenericFooIn<SomeClassIn> {}

fun usage(x: SomeClassIn) {
    <expr>x</expr>
}
