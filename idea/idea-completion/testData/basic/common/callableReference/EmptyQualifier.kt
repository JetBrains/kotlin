fun globalFun(p: Int) {}

fun String.extensionFun(){}
val String.extensionVal: Int
    get() = 1

val globalVal = 1
var globalVar = 1

class C {
    fun memberFun(){}

    val memberVal = 1

    class NestedClass
    inner class InnerClass

    fun foo() {
        fun localFun(){}

        val local = 1

        val v = ::<caret>
    }

    companion object {
        fun companionObjectFun(){}
    }
}

class WithPrivateConstructor private constructor()
abstract class AbstractClass

// EXIST: { itemText: "globalFun", attributes: "" }
// EXIST: { itemText: "globalVal", attributes: "" }
// EXIST: { itemText: "globalVar", attributes: "" }
// ABSENT: extensionFun
// ABSENT: extensionVal
// EXIST: { itemText: "memberFun", attributes: "bold" }
// EXIST: { itemText: "memberVal", attributes: "bold" }
// EXIST: { itemText: "localFun", attributes: "" }
// ABSENT: { itemText: "local", attributes: "" }
// EXIST: { itemText: "companionObjectFun", attributes: "bold" }
// EXIST: { itemText: "C", attributes: "" }
// EXIST: { itemText: "NestedClass", attributes: "" }
// EXIST: { itemText: "InnerClass", attributes: "bold" }
// ABSENT: WithPrivateConstructor
// ABSENT: AbstractClass
