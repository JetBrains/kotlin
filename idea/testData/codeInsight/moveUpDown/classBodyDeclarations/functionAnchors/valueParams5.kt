// MOVE: down
// MOVER_CLASS: org.jetbrains.kotlin.idea.codeInsight.upDownMover.JetExpressionMover
// IS_APPLICABLE: false
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
