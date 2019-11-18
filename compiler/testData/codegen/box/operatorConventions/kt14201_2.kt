// IGNORE_BACKEND_FIR: JVM_IR
class A {
    val z: String = "OK"
}

class B {
    operator fun A.invoke(): String = z
}

class ClassB {
    val x = A()

    fun B.test(): String {
        val value = object {
            val z = x()
        }
        return value.z
    }

    fun call(): String {
        return B().test()
    }

}

fun box(): String {
    return ClassB().call()
}