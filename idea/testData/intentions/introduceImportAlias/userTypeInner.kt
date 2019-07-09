class Outer {
    class Middle<T> {}
    class Middle1 {}
}

fun main() {
    val t = Outer.Middle<Outer.Middle<caret><Outer.Middle1>>()
}
