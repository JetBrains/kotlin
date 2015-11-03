import java.io.File

interface I {
    fun abstractFun()
    val abstractVal: Int
    fun nonAbstractFun(){}
}

fun I.extOnI(){}
val File.extOnFile: Int get() = 1

open class Base : File("") {
    class Nested
    inner class Inner

    open fun fromBase1(): Any = 1
    open fun fromBase2(): Any = 1
}

abstract class A : Base(), I {
    override fun fromI() {
        super<Base>.<caret>
    }

    override fun fromBase1(): String = ""
}

// ABSENT: abstractFun
// ABSENT: abstractVal
// ABSENT: nonAbstractFun
// EXIST: { itemText: "equals", attributes: "" }
// EXIST: { itemText: "hashCode", attributes: "" }

// EXIST: { itemText: "fromBase1", typeText: "Any", attributes: "bold" }
// ABSENT: { itemText: "fromBase1", typeText: "String" }
// EXIST: { itemText: "fromBase2", typeText: "Any", attributes: "bold" }

// ABSENT: extOnI
// ABSENT: extOnFile

// EXIST_JAVA_ONLY: { itemText: "getAbsolutePath", attributes: "" }
// ABSENT: absolutePath