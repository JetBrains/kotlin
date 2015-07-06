fun C.extFunForC(){}
fun T.extFunForT(){}
fun Any.extFunForAny(){}

open class C {
    fun foo() {
        if (this is T) {
            <caret>
        }
    }
}

trait T

// EXIST: { itemText: "extFunForT", attributes: "bold" }
// EXIST: { itemText: "extFunForC", attributes: "bold" }
// EXIST: { itemText: "extFunForAny", attributes: "" }
