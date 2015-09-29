class C {
    fun memberFun(){}

    val memberVal = 1

    class NestedClass
    inner class InnerClass

    companion object {
        fun companionObjectFun(){}
    }
}

fun C.foo() {
    val v = ::<caret>
}

// EXIST: { itemText: "memberFun", attributes: "bold" }
// EXIST: { itemText: "memberVal", attributes: "bold" }
// EXIST: { itemText: "hashCode", attributes: "" }
// ABSENT: companionObjectFun
// ABSENT: NestedClass
// EXIST: { itemText: "InnerClass", attributes: "bold" }
