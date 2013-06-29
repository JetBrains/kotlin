// MOVE: up
// MOVER_CLASS: org.jetbrains.jet.plugin.codeInsight.upDownMover.JetExpressionMover
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