package test

class Person(val name: String) {

    fun sayName() = doSayName { name }

    inline fun doSayName(crossinline call: () -> String): String {
        return nestedSayName1 { name + Person("sub").nestedSayName2 { call() } }
    }

    fun nestedSayName1(call: () -> String) = call()

    inline fun nestedSayName2(call: () -> String)  = name + call()
}
