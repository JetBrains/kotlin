// IGNORE_BACKEND_FIR: JVM_IR
package a.b

interface Test {
    fun invoke(): String {
        return "OK"
    }
}

private val a : Test = {
    object : Test {

    }
}()

fun box(): String {
    return a.invoke();
}