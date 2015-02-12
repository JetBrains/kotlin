// MOVE: up
// MOVER_CLASS: org.jetbrains.kotlin.idea.codeInsight.upDownMover.JetExpressionMover
class A {
    fun foo<T,
            U,
            W>(
            b: Int,
            c: Int
            <caret>a: Int,
    ) {

    }
    class B {

    }
}
