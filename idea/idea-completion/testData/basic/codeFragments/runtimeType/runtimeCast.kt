fun main(args: Array<String>) {
    val b: Base = Derived()
    <caret>val a = 1
}

open class Base {
    fun funInBase() {}

    open fun funWithOverride() { }
    open fun funWithoutOverride() { }
}

class Derived: Base() {
    fun funInDerived() { }

    override fun funWithOverride() { }
}

// INVOCATION_COUNT: 1
// EXIST: funInBase, funWithOverride, funWithoutOverride, funInDerived
// NOTHING_ELSE: true


// RUNTIME_TYPE: Derived