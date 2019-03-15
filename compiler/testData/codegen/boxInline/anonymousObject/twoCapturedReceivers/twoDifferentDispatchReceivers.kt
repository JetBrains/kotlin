// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

class Company(val name: String) {
    fun sayName() = Person("test").doSayName { name }
}

class Person(val name: String) {

    inline fun doSayName(crossinline call: () -> String): String {
        return companyName { parsonName { call() } }
    }

    inline fun parsonName(call: () -> String) = call()

    fun companyName(call: () -> String) = call()

}

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
import test.*

fun box(): String {
    return Company("OK").sayName()
}
