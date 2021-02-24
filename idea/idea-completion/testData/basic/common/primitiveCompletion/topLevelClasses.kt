// FIR_COMPARISON
class DefaultConstructor

class TwoConstructors(s: String) {
    constructor(l: Int): this(l.toString()) {}
}

data class DataClass(val a: Int)

abstract class AbstractClass

fun usage() {
    <caret>
}

// EXIST: DefaultConstructor
// EXIST: TwoConstructors
// EXIST: DataClass
// EXIST: AbstractClass