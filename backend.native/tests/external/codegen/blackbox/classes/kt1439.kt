class MyClass(var fnc : () -> String) {

    fun test(): String {
        return fnc()
    }

}

fun printtest() : String {
    return "OK"
}

fun box(): String {
    var c = MyClass({ printtest() })

    return c.test()
}
