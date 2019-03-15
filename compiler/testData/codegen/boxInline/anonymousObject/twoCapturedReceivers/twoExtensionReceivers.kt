// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

fun Person.sayName() = doSayName { name }

class Person(val name: String)

inline fun Person.doSayName(crossinline call: () -> String): String {
    return companyName { parsonName { call() } }
}

inline fun Person.parsonName(call: () -> String) = call()

fun Person.companyName(call: () -> String) = call()

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
import test.*

fun box(): String {
    return Person("OK").sayName()
}
