// MOVE: down
// MOVER_CLASS: org.jetbrains.jet.plugin.codeInsight.upDownMover.JetExpressionMover
class A {
    fun foo<T,
            U,
            W>(
            b: Int,
            <caret>a: Int,
            c: Int
    ) {

    }
    class B {

    }
}