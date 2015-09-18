interface T {
    fun fromTrait() = ""
}

abstract class Base : T {
    fun fromBase() = ""
}

class Derived : Base() {
    override fun fromTrait() = ""

    val fromDerived: String = ""
}

fun foo(d: Derived): String {
    return d.<caret>
}

// EXIST: { itemText: "fromTrait", attributes: "" }
// EXIST: { itemText: "fromDerived", attributes: "bold" }
// EXIST: { itemText: "fromBase", attributes: "" }
// EXIST: { itemText: "toString", attributes: "" }
