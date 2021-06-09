// FIR_COMPARISON
fun String.extFunForString(){}
fun Any.extFunForAny(){}
fun String?.extFunForStringNullable(){}

class C {
    fun foo() {
        System.getProperty("a").<caret>
    }
}

// EXIST: { itemText: "extFunForString", attributes: "bold" }
// EXIST: { itemText: "extFunForAny", attributes: "" }
// EXIST: { itemText: "extFunForStringNullable", attributes: "bold" }
// EXIST: { itemText: "get", attributes: "" }
