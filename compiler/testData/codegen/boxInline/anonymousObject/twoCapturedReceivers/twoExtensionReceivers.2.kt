package test

fun Person.sayName() = doSayName { name }

class Person(val name: String)

inline fun Person.doSayName(crossinline call: () -> String): String {
    return companyName { parsonName { call() } }
}

inline fun Person.parsonName(call: () -> String) = call()

fun Person.companyName(call: () -> String) = call()

