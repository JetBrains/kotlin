// MOVE: up
// MOVER_CLASS: org.jetbrains.kotlin.idea.codeInsight.upDownMover.JetExpressionMover
class A {
    fun foo<T,
            U,
            W>(
            a: Int,
            b:<caret> Int,
            c: Int
    ) {

    }
    class B {

    }
}
