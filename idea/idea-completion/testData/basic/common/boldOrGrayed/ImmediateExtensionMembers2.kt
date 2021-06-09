// FIR_COMPARISON
fun String.extFunForString(){}
fun Any.extFunForAny(){}
fun String?.extFunForStringNullable(){}

class C {
    fun foo(s: String?) {
        if (s != null) {
            s.<caret>
        }
    }
}

// EXIST: { itemText: "extFunForString", attributes: "bold" }
// EXIST: { itemText: "extFunForAny", attributes: "" }
// EXIST: { itemText: "extFunForStringNullable", attributes: "" }
