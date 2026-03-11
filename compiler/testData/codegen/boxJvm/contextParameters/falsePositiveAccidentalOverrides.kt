// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +ContextParameters
// WITH_STDLIB
// ISSUE: KT-76588

open class ParentA {
    fun testA(x: String): String = "testA"
}
class ChildA: ParentA() {
    @JvmName("ctxTestA")
    context(_: String) fun testA(): String = "ctxTestA"
}

interface ParentB {
    fun testB(x: String): String = "testB"
}
class ChildB: ParentB {
    @JvmName("ctxTestB")
    context(_: String) fun testB(): String = "ctxTestB"
}

interface ParentC {
    fun testC(x: String): String
}
class ChildC: ParentC {
    override fun testC(x: String): String = "testC"
    @JvmName("ctxTestC")
    context(_: String) fun testC(): String = "ctxTestC"
}

class ChildD {
    @JvmName("ctxTestD")
    context(_: String) fun testD(): String = "ctxTestD"
}

fun box(): String {
    ChildA().testA("").let { result -> if (result != "testA") return "NOT OK: testA() == $result" }
    ChildB().testB("").let { result -> if (result != "testB") return "NOT OK: testB() == $result" }
    ChildC().testC("").let { result -> if (result != "testC") return "NOT OK: testC() == $result" }

    with("") {
        ChildA().testA().let { result -> if (result != "ctxTestA") return "NOT OK: ctxTestA() == $result" }
        ChildB().testB().let { result -> if (result != "ctxTestB") return "NOT OK: ctxTestB() == $result" }
        ChildC().testC().let { result -> if (result != "ctxTestC") return "NOT OK: ctxTestC() == $result" }
        ChildD().testD().let { result -> if (result != "ctxTestD") return "NOT OK: ctxTestD() == $result" }
    }

    return "OK"
}
