import java.io.File

interface I {
    fun abstractFun()
    val abstractVal: Int
    fun nonAbstractFun(){}
}

fun I.extOnI(){}
val File.extOnFile: Int get() = 1

interface J {
    fun funFromJ()
    fun onLambda1(p: () -> Unit){}
    fun onLambda2(p: (Int, String) -> Unit){}
}

open class Base : File(""), J {
    class Nested
    inner class Inner

    open fun fromBase1(): Any = 1
    open fun fromBase2(): Any = 1
}

abstract class A : Base(), I {
    override fun abstractFun() {
        super.<caret>
    }

    override fun fromBase1(): String = ""
}

// ABSENT: abstractFun
// ABSENT: abstractVal
// EXIST: { itemText: "nonAbstractFun", attributes: "bold" }
// EXIST: { itemText: "equals", attributes: "" }
// EXIST: { itemText: "hashCode", attributes: "" }

// EXIST: { itemText: "fromBase1", typeText: "Any", attributes: "bold" }
// ABSENT: { itemText: "fromBase1", typeText: "String" }
// EXIST: { itemText: "fromBase2", typeText: "Any", attributes: "bold" }

// ABSENT: extOnI
// ABSENT: extOnFile

// ABSENT: funFromJ

// EXIST_JAVA_ONLY: { itemText: "getAbsolutePath", attributes: "" }
// ABSENT: absolutePath

// EXIST: { itemText: "onLambda1", tailText: "(p: () -> Unit)", attributes: "" }
// EXIST: { itemText: "onLambda2", tailText: "(p: (Int, String) -> Unit)", attributes: "" }
// ABSENT: { itemText: "onLambda2", tailText: " { Int, String -> ... } (p: (Int, String) -> Unit)" }
