// FIR_COMPARISON
fun String.extFunForString(){}
fun Any.extFunForAny(){}
fun String?.extFunForStringNullable(){}

class C {
    fun Any.memberExtFunForAny(){}
    fun String?.memberExtFunForStringNullable(){}

    fun foo() {
        "".<caret>
    }
}

// EXIST: { itemText: "extFunForString", attributes: "bold" }
// EXIST: { itemText: "extFunForAny", attributes: "" }
// EXIST: { itemText: "extFunForStringNullable", attributes: "" }
// EXIST: { itemText: "memberExtFunForAny", attributes: "" }
// EXIST: { itemText: "memberExtFunForStringNullable", attributes: "" }
