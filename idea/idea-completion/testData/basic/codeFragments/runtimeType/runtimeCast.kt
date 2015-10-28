fun main(args: Array<String>) {
    val b: Base = Derived()
    <caret>val a = 1
}

open class Base {
    fun funInBase() {}

    open fun funWithOverride() { }
    open fun funWithoutOverride() { }

    fun funInBoth() { }
}

class Derived: Base() {
    fun funInDerived() { }

    override fun funWithOverride() { }

    fun funInBoth(p: Int) { }
}

// INVOCATION_COUNT: 1
// EXIST: funInBase
// EXIST: funWithOverride
// EXIST: funWithoutOverride
// EXIST: funInDerived
// EXIST: { itemText: "funInBoth", tailText: "()", attributes: "bold" }
// EXIST: { itemText: "funInBoth", tailText: "(p: Int)", attributes: "grayed" }
// NOTHING_ELSE


// RUNTIME_TYPE: Derived