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
