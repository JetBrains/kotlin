interface I1 {
    fun i1Member()
}

interface I2 {
    fun i2Member()
}

fun I1.i1Extension(){}
fun I2.i2Extension(){}
fun Any.anyExtension(){}
fun String.stringExtension(){}

open class C {
    fun cMember(){}

    fun foo() {
        if (this is I1 && this is I2) {
            val v = ::<caret>
        }
    }
}

// EXIST: { itemText: "i1Extension", attributes: "bold" }
// EXIST: { itemText: "i2Extension", attributes: "bold" }
// EXIST: { itemText: "i1Member", attributes: "bold" }
// EXIST: { itemText: "i2Member", attributes: "bold" }
// EXIST: { itemText: "anyExtension", attributes: "" }
// EXIST: { itemText: "hashCode", attributes: "" }
// ABSENT: stringExtension
// EXIST: { itemText: "cMember", attributes: "bold" }
