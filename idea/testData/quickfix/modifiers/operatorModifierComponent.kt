// "Add 'operator' modifier" "true"
class A {
    fun component1(): Int = 0
    fun component2(): Int = 1
}

fun foo() {
    val (<caret>zero, one) = A()
}

