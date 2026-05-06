interface GenericFooIn<in T>

fun <R: GenericFooIn<R>> GenericFooIn<R>.ext() {
    th<caret_1_target>is
}

class SomeClassIn: GenericFooIn<SomeClassIn> {}

fun usage(xx: SomeClassIn) {
    x<caret_1_base>x
}
