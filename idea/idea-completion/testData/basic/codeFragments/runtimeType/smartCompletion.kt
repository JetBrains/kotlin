fun main(args: Array<String>) {
    val b: Base = Derived()
    <caret>val a = 1
}

open class Base {
    fun funInBase(): Int = 0

    open fun funWithOverride(): Int = 0
    open fun funWithoutOverride(): Int = 0

    fun funInBoth(): Int = 0
}

open class Intermediate : Base() {
    fun funInIntermediate() = 0
}

class Derived : Intermediate() {
    fun funInDerived(): Int = 0

    override fun funWithOverride(): Int = 0

    fun funInBoth(p: Int): Int = 0

    fun funWrongType(): String = ""
}

// COMPLETION_TYPE: SMART
// INVOCATION_COUNT: 1
// EXIST: { itemText: "funInBase", tailText: "()", attributes: "bold" }
// EXIST: { itemText: "funWithOverride", tailText: "()", attributes: "bold" }
// EXIST: { itemText: "funWithoutOverride", tailText: "()", attributes: "bold" }
// EXIST: { itemText: "funInDerived", tailText: "()", attributes: "bold" }
// EXIST: { itemText: "funInBoth", tailText: "()", attributes: "bold" }
// EXIST: { itemText: "funInBoth", tailText: "(p: Int)", attributes: "bold" }
// EXIST: { itemText: "funInIntermediate", tailText: "()", attributes: "" }
// NOTHING_ELSE


// RUNTIME_TYPE: Derived