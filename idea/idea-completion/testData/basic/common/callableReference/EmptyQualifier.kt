fun globalFun(p: Int) {}

fun String.extensionFun(){}
val String.extensionVal: Int
    get() = 1

val globalVal = 1
var globalVar = 1

fun funWithFunctionParameter(p: () -> Unit) {}

fun <T> genericFun(t: T): T = t

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
// ABSENT: memberFun
// ABSENT: memberVal
// EXIST: { itemText: "localFun", attributes: "" }
// ABSENT: local
// ABSENT: companionObjectFun
// EXIST: { itemText: "C", attributes: "" }
// EXIST: { itemText: "NestedClass", attributes: "" }
// ABSENT: InnerClass
// ABSENT: WithPrivateConstructor
// ABSENT: AbstractClass
// ABSENT: class
// ABSENT: class.java
// EXIST: { itemText: "funWithFunctionParameter", tailText: "(p: () -> Unit) (<root>)", attributes: "" }
// EXIST: { itemText: "genericFun", tailText: "(t: T) (<root>)", attributes: "" }

// ABSENT: "kotlin"
// ABSENT: "java"
