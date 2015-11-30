// "Change 'A.component2' function return type to 'Int'" "true"
abstract class A {
    abstract operator fun component1(): Int
    abstract operator fun component2(): String
}

fun foo(a: A) {
    val (w: Int, x: Int) = a<caret>
}