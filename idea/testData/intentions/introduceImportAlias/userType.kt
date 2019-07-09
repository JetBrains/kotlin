class Outer {
    class Middle<T> {}
}

class B<T> {}

fun foo(b: B<Outer.Middle<caret><String>>) {
}
