// ISSUE: KT-59715

class FunctionComponent {
    val component1: () -> String = { "hello" }
}

class I {
    operator fun invoke(): String = "hello"
}

class InvokeComponent {
    val component1: I = I()
}

fun test_1(c: FunctionComponent) {
    val (x) = FunctionComponent()
    val (y) = c
}

fun test_2(c: InvokeComponent) {
    val (x) = FunctionComponent()
    val (y) = c
}
