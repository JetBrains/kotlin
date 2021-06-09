// FIR_COMPARISON
interface T {
    fun fromTrait(){}
}

abstract class Base : T {
    fun fromBase(){}
}

class Derived : Base() {
    override fun fromTrait() { }

    fun fromDerived(){}
}

fun foo(d: Derived) {
    d.<caret>
}

// EXIST: { itemText: "fromTrait", attributes: "" }
// EXIST: { itemText: "fromDerived", attributes: "bold" }
// EXIST: { itemText: "fromBase", attributes: "" }
// EXIST: { itemText: "hashCode", attributes: "" }
// EXIST: { itemText: "equals", attributes: "" }
