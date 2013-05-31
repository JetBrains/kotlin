// "Remove explicitly specified return type in 'A.component2' function" "true"
abstract class A {
    abstract fun component1(): Int
    abstract fun component2(): Int
}

fun foo(a: A) {
    val (w: Int, x: Unit) = a<caret>
}