// FIR_COMPARISON
fun globalFun(){}

interface T {
    fun fromTrait(){}
}

abstract class Base : T {
    fun fromBase(){}
}

class Derived : Base() {
    override fun fromTrait() { }

    fun fromDerived(){}

    fun foo(d: Derived) {
        <caret>
    }
}

// EXIST: { itemText: "foo", attributes: "bold" }
// EXIST: { itemText: "fromTrait", attributes: "" }
// EXIST: { itemText: "fromDerived", attributes: "bold" }
// EXIST: { itemText: "fromBase", attributes: "" }
// EXIST: { itemText: "hashCode", attributes: "" }
// EXIST: { itemText: "equals", attributes: "" }
// EXIST: { itemText: "globalFun", attributes: "" }
