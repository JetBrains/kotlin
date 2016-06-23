abstract class A {
    abstract fun memberFunInA()
    abstract val memberValInA: Int

    inner class InnerInA
    class NestedInA
}

abstract class B : A() {
    abstract fun memberFunInB()
    abstract val memberValInB: Int

    inner class InnerInB
    class NestedInB
}

fun A.extensionFun(){}

val A.extensionVal: Int
    get() = 1

fun B.extensionFunForB(){}

fun Any.anyExtensionFun(){}
fun String.wrongExtensionFun(){}

fun globalFun(p: Int) {}
val globalVal = 1

class C {
    fun memberFun(){}

    val memberVal = 1

    fun A.memberExtensionFun(){}

    fun foo(a: A) {
        fun localFun(){}

        if (a is B) {
            val v = a::<caret>
        }
    }

    companion object {
        fun companionObjectFun(){}

        fun A.companionExtension(){}
    }
}

// EXIST: class
// EXIST_JAVA_ONLY: class.java
// EXIST: { itemText: "memberFunInA", attributes: "" }
// EXIST: { itemText: "memberValInA", attributes: "" }

/*TODO!*/
// ABSENT: { itemText: "InnerInA", attributes: "" }

// ABSENT: NestedInA
// EXIST: { itemText: "extensionFun", attributes: "" }
// EXIST: { itemText: "extensionVal", attributes: "" }
// EXIST: { itemText: "anyExtensionFun", attributes: "" }
// ABSENT: wrongExtensionFun
// ABSENT: globalFun
// ABSENT: globalVal
// ABSENT: memberFun
// ABSENT: memberVal
// ABSENT: memberExtensionFun
// ABSENT: localFun
// ABSENT: companionObjectFun
// ABSENT: companionExtension

// EXIST: { itemText: "memberFunInB", attributes: "bold" }
// EXIST: { itemText: "memberValInB", attributes: "bold" }
// EXIST: { itemText: "InnerInB", attributes: "bold" }
// ABSENT: NestedInB
// EXIST: { itemText: "extensionFunForB", attributes: "bold" }
