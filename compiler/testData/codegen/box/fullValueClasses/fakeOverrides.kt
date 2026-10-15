// LANGUAGE: +FullValueClasses
// WITH_STDLIB

abstract value class Base {
    abstract val first: String

    fun fakeOverridden(): String = first
}

abstract value class Child() : Base()

fun callOnChild(child: Child): String = child.fakeOverridden()

value class Grandchild(override val first: String, val second: String) : Child()

fun box(): String {
    return callOnChild(Grandchild("O", "K")) + Grandchild("K", "O").fakeOverridden()
}
