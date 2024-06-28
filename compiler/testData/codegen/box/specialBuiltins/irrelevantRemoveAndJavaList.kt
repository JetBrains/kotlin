// TARGET_BACKEND: JVM
// FULL_JDK

import java.util.LinkedList

interface KotlinInterface {
    fun remove(i: Int): Boolean
}

var result = "Fail"

abstract class C : LinkedList<Int>(), KotlinInterface

class D : C() {
    override fun remove(i: Int): Boolean {
        result = "OK"
        return true
    }
}

fun box(): String {
    D().remove(0)
    return result
}
